package com.sphynxs.mydatabases.core.database.ssh

/**
 * Base exception for SSH tunnel-related errors.
 *
 * @param message Error message
 * @param cause Original exception that caused this error
 */
sealed class SSHTunnelException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    
    /**
     * SSH connection timeout - couldn't reach SSH host within timeout period.
     */
    class ConnectionTimeout(host: String, port: Int, cause: Throwable? = null) : SSHTunnelException(
        "SSH connection timed out. Could not reach $host:$port",
        cause
    )
    
    /**
     * SSH authentication failed - invalid credentials (password or private key).
     */
    class AuthenticationFailed(username: String, authMethod: String, cause: Throwable? = null) : SSHTunnelException(
        "SSH authentication failed for user '$username' using $authMethod",
        cause
    )
    
    /**
     * Invalid SSH private key format or encrypted key.
     */
    class InvalidKey(message: String, cause: Throwable? = null) : SSHTunnelException(message, cause)
    
    /**
     * Cannot allocate local port for SSH tunnel (all ports in use or bind failure).
     */
    class PortAllocationFailed(attempts: Int, cause: Throwable? = null) : SSHTunnelException(
        "Cannot allocate local port for SSH tunnel after $attempts attempts",
        cause
    )
    
    /**
     * SSH tunnel connection dropped unexpectedly.
     */
    class TunnelDropped(message: String, cause: Throwable? = null) : SSHTunnelException(message, cause)
    
    /**
     * Generic SSH tunnel error (catch-all for unexpected errors).
     */
    class Generic(message: String, cause: Throwable? = null) : SSHTunnelException(message, cause)
}
