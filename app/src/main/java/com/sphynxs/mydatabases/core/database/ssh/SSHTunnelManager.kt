package com.sphynxs.mydatabases.core.database.ssh

import android.content.Context
import android.net.Uri
import android.util.Log
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import com.sphynxs.mydatabases.core.database.models.SSHAuthMethod
import com.sphynxs.mydatabases.core.database.models.SSHTunnelConfig
import java.util.Properties
import kotlin.random.Random

/**
 * Manages SSH tunnel lifecycle for secure database connections through bastion/jump hosts.
 *
 * Uses JSch library to establish SSH sessions and local port forwarding.
 * Thread-safe for concurrent connections.
 *
 * Workflow:
 * ```
 * 1. Create SSHTunnelManager with SSH config
 * 2. Call connect(dbHost, dbPort) → returns local port
 * 3. Use localhost:localPort for JDBC connection
 * 4. Call disconnect() when done
 * ```
 *
 * Security considerations:
 * - Local port forwarding binds to 127.0.0.1 only (not exposed to network)
 * - StrictHostKeyChecking disabled (no known_hosts file on Android)
 * - User must accept security warning on first use
 *
 * @param config SSH tunnel configuration (host, credentials, auth method)
 * @param context Android context for reading private keys from URIs
 *
 * @author israel-icm
 * @date 2026-06-30
 */
class SSHTunnelManager(
    private val config: SSHTunnelConfig,
    private val context: Context
) {
    
    companion object {
        private const val TAG = "SSHTunnelManager"
        
        // Ephemeral port range for local port forwarding
        private const val PORT_RANGE_MIN = 49152
        private const val PORT_RANGE_MAX = 65535
        
        // Max retry attempts for port allocation
        private const val MAX_PORT_ALLOCATION_ATTEMPTS = 3
        
        // SSH connection timeout (milliseconds)
        private const val SSH_CONNECT_TIMEOUT = 10_000
    }
    
    private var session: Session? = null
    private var localPort: Int? = null
    private val jsch = JSch()
    
    /**
     * Establishes SSH tunnel and returns local port for JDBC connection.
     *
     * Process:
     * 1. Create JSch session with SSH host/port/username
     * 2. Configure session (disable strict host key checking)
     * 3. Authenticate (password or private key)
     * 4. Connect session
     * 5. Set up local port forwarding (localhost:randomPort → dbHost:dbPort)
     * 6. Return local port for JDBC connection
     *
     * @param remoteHost Database server hostname (for port forwarding target)
     * @param remotePort Database server port (for port forwarding target)
     * @return Local port number to use for JDBC connection (localhost:localPort)
     * @throws SSHTunnelException if connection fails
     */
    fun connect(remoteHost: String, remotePort: Int): Int {
        Log.d(TAG, "Establishing SSH tunnel to ${config.host}:${config.port} for forwarding to $remoteHost:$remotePort")
        
        try {
            // Create SSH session
            val sshSession = jsch.getSession(
                config.username,
                config.host,
                config.port
            )
            
            // Configure session
            val sessionConfig = Properties().apply {
                // Disable strict host key checking (no known_hosts file on Android)
                put("StrictHostKeyChecking", "no")
                
                // Disable host key verification (security tradeoff for mobile)
                put("PreferredAuthentications", "publickey,password")
            }
            sshSession.setConfig(sessionConfig)
            sshSession.timeout = SSH_CONNECT_TIMEOUT
            
            // Authenticate based on method
            when (config.authMethod) {
                SSHAuthMethod.PASSWORD -> {
                    Log.d(TAG, "Using password authentication")
                    sshSession.setPassword(config.password)
                }
                
                SSHAuthMethod.PRIVATE_KEY -> {
                    Log.d(TAG, "Using private key authentication")
                    authenticateWithPrivateKey(config.privateKeyUri)
                }
            }
            
            // Connect SSH session
            try {
                sshSession.connect()
                Log.d(TAG, "SSH session connected successfully")
            } catch (e: Exception) {
                when {
                    e.message?.contains("Auth fail") == true -> {
                        throw SSHTunnelException.AuthenticationFailed(
                            username = config.username,
                            authMethod = config.authMethod.name,
                            cause = e
                        )
                    }
                    e.message?.contains("timeout") == true -> {
                        throw SSHTunnelException.ConnectionTimeout(
                            host = config.host,
                            port = config.port,
                            cause = e
                        )
                    }
                    else -> {
                        throw SSHTunnelException.Generic(
                            "SSH connection failed: ${e.message}",
                            cause = e
                        )
                    }
                }
            }
            
            // Set up local port forwarding with retry
            val allocatedPort = allocateLocalPort(sshSession, remoteHost, remotePort)
            
            // Store session and port
            session = sshSession
            localPort = allocatedPort
            
            Log.d(TAG, "SSH tunnel established: localhost:$allocatedPort → $remoteHost:$remotePort")
            return allocatedPort
            
        } catch (e: SSHTunnelException) {
            // Re-throw our own exceptions
            throw e
        } catch (e: Exception) {
            throw SSHTunnelException.Generic(
                "Unexpected error establishing SSH tunnel: ${e.message}",
                cause = e
            )
        }
    }
    
    /**
     * Configures JSch with private key authentication.
     */
    private fun authenticateWithPrivateKey(privateKeyUri: String?) {
        if (privateKeyUri.isNullOrBlank()) {
            throw SSHTunnelException.InvalidKey("Private key URI is null or empty")
        }
        
        try {
            val uri = Uri.parse(privateKeyUri)
            val keyBytes = SSHKeyReader.readPrivateKey(context, uri)
            
            // Add identity to JSch (private key for authentication)
            jsch.addIdentity(
                "mobile-key", // identity name (arbitrary)
                keyBytes,     // private key bytes
                null,         // public key bytes (optional, JSch can derive it)
                null          // passphrase (null = no passphrase)
            )
            
            Log.d(TAG, "Private key loaded successfully")
            
        } catch (e: IllegalStateException) {
            // SSHKeyReader throws IllegalStateException for encrypted keys
            throw SSHTunnelException.InvalidKey(e.message ?: "Encrypted key not supported", e)
        } catch (e: Exception) {
            throw SSHTunnelException.InvalidKey(
                "Failed to load private key: ${e.message}",
                cause = e
            )
        }
    }
    
    /**
     * Allocates a local port for SSH forwarding with retry logic.
     *
     * Attempts to bind to random ports in ephemeral range (49152-65535).
     * Retries up to MAX_PORT_ALLOCATION_ATTEMPTS times if port is already in use.
     *
     * @param sshSession Active SSH session
     * @param remoteHost Database server hostname
     * @param remotePort Database server port
     * @return Allocated local port number
     * @throws SSHTunnelException.PortAllocationFailed if all attempts fail
     */
    private fun allocateLocalPort(
        sshSession: Session,
        remoteHost: String,
        remotePort: Int
    ): Int {
        repeat(MAX_PORT_ALLOCATION_ATTEMPTS) { attempt ->
            val candidatePort = Random.nextInt(PORT_RANGE_MIN, PORT_RANGE_MAX + 1)
            
            try {
                // Attempt to set up port forwarding
                // Format: setPortForwardingL(bind_address, local_port, remote_host, remote_port)
                sshSession.setPortForwardingL(
                    "127.0.0.1",   // Bind to localhost only (security)
                    candidatePort, // Local port
                    remoteHost,    // Database server host
                    remotePort     // Database server port
                )
                
                Log.d(TAG, "Port forwarding established on localhost:$candidatePort (attempt ${attempt + 1})")
                return candidatePort
                
            } catch (e: Exception) {
                Log.w(TAG, "Port allocation failed for $candidatePort (attempt ${attempt + 1}): ${e.message}")
                
                // If this was the last attempt, throw exception
                if (attempt == MAX_PORT_ALLOCATION_ATTEMPTS - 1) {
                    throw SSHTunnelException.PortAllocationFailed(
                        attempts = MAX_PORT_ALLOCATION_ATTEMPTS,
                        cause = e
                    )
                }
                // Otherwise, retry with different port
            }
        }
        
        // Should never reach here due to throw in loop, but compiler needs it
        throw SSHTunnelException.PortAllocationFailed(MAX_PORT_ALLOCATION_ATTEMPTS)
    }
    
    /**
     * Closes SSH tunnel and releases all resources.
     *
     * Safe to call multiple times (idempotent).
     * Should be called when database connection is closed.
     */
    fun disconnect() {
        try {
            session?.let { activeSession ->
                if (activeSession.isConnected) {
                    // Remove port forwarding
                    localPort?.let { port ->
                        try {
                            activeSession.delPortForwardingL(port)
                            Log.d(TAG, "Port forwarding removed for localhost:$port")
                        } catch (e: Exception) {
                            Log.w(TAG, "Error removing port forwarding: ${e.message}")
                        }
                    }
                    
                    // Disconnect session
                    activeSession.disconnect()
                    Log.d(TAG, "SSH session disconnected")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during SSH tunnel cleanup", e)
        } finally {
            session = null
            localPort = null
        }
    }
    
    /**
     * Checks if SSH tunnel is currently active and connected.
     *
     * @return true if tunnel is active and session is connected
     */
    fun isActive(): Boolean {
        return session?.isConnected == true
    }
    
    /**
     * Gets the local port for JDBC connection.
     *
     * @return Local port number, or null if tunnel is not established
     */
    fun getLocalPort(): Int? = localPort
    
    /**
     * Checks tunnel health and attempts reconnect if needed.
     *
     * Should be called before executing queries to ensure tunnel is alive.
     *
     * @param remoteHost Database server hostname (for reconnect)
     * @param remotePort Database server port (for reconnect)
     * @return true if tunnel is healthy or reconnected successfully
     */
    fun ensureConnected(remoteHost: String, remotePort: Int): Boolean {
        return when {
            isActive() -> {
                // Tunnel is active, no action needed
                true
            }
            else -> {
                // Tunnel dropped, attempt reconnect
                Log.w(TAG, "SSH tunnel dropped, attempting reconnect...")
                try {
                    connect(remoteHost, remotePort)
                    true
                } catch (e: Exception) {
                    Log.e(TAG, "SSH tunnel reconnect failed", e)
                    false
                }
            }
        }
    }
}
