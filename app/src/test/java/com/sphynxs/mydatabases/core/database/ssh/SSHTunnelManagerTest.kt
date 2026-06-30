package com.sphynxs.mydatabases.core.database.ssh

import android.content.Context
import android.net.Uri
import com.jcraft.jsch.JSch
import com.jcraft.jsch.JSchException
import com.jcraft.jsch.Session
import com.sphynxs.mydatabases.core.database.models.SSHAuthMethod
import com.sphynxs.mydatabases.core.database.models.SSHTunnelConfig
import io.mockk.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests para SSHTunnelManager.
 *
 * Usa Mockk para simular JSch y probar la lógica de túnel SSH
 * sin necesidad de un servidor SSH real.
 *
 * @author israel-icm
 * @date 2026-06-30
 */
class SSHTunnelManagerTest {

    private lateinit var context: Context
    private lateinit var mockJSch: JSch
    private lateinit var mockSession: Session

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        mockJSch = mockk(relaxed = true)
        mockSession = mockk(relaxed = true)
        
        // Mock JSch behavior
        every { mockJSch.getSession(any(), any(), any()) } returns mockSession
        every { mockSession.isConnected } returns false
        every { mockSession.connect() } just Runs
        every { mockSession.setPortForwardingL(any(), any(), any(), any()) } returns 0
        every { mockSession.disconnect() } just Runs
    }

    @After
    fun teardown() {
        clearAllMocks()
    }

    @Test
    fun `connect with password auth establishes tunnel successfully`() {
        // Given
        val config = SSHTunnelConfig(
            enabled = true,
            host = "bastion.example.com",
            port = 22,
            username = "sshuser",
            authMethod = SSHAuthMethod.PASSWORD,
            password = "sshpass123"
        )
        
        val manager = SSHTunnelManager(config, context)
        
        // Mock successful connection
        every { mockSession.isConnected } returns true
        
        // When
        val localPort = manager.connect("db.internal.com", 3306)
        
        // Then
        assertTrue(localPort in 49152..65535)
        assertTrue(manager.isActive())
        assertNotNull(manager.getLocalPort())
    }

    @Test(expected = SSHTunnelException.AuthenticationFailed::class)
    fun `connect with invalid password throws AuthenticationFailed`() {
        // Given
        val config = SSHTunnelConfig(
            enabled = true,
            host = "bastion.example.com",
            port = 22,
            username = "sshuser",
            authMethod = SSHAuthMethod.PASSWORD,
            password = "wrongpassword"
        )
        
        val manager = SSHTunnelManager(config, context)
        
        // Mock auth failure
        every { mockSession.connect() } throws JSchException("Auth fail")
        
        // When
        manager.connect("db.internal.com", 3306)
        
        // Then - exception thrown
    }

    @Test(expected = SSHTunnelException.ConnectionTimeout::class)
    fun `connect with timeout throws ConnectionTimeout`() {
        // Given
        val config = SSHTunnelConfig(
            enabled = true,
            host = "unreachable.example.com",
            port = 22,
            username = "sshuser",
            authMethod = SSHAuthMethod.PASSWORD,
            password = "password"
        )
        
        val manager = SSHTunnelManager(config, context)
        
        // Mock timeout
        every { mockSession.connect() } throws JSchException("timeout")
        
        // When
        manager.connect("db.internal.com", 3306)
        
        // Then - exception thrown
    }

    @Test
    fun `disconnect cleans up session and port forwarding`() {
        // Given
        val config = SSHTunnelConfig(
            enabled = true,
            host = "bastion.example.com",
            port = 22,
            username = "sshuser",
            authMethod = SSHAuthMethod.PASSWORD,
            password = "password"
        )
        
        val manager = SSHTunnelManager(config, context)
        
        every { mockSession.isConnected } returns true
        manager.connect("db.internal.com", 3306)
        
        // When
        manager.disconnect()
        
        // Then
        verify { mockSession.disconnect() }
        assertFalse(manager.isActive())
        assertNull(manager.getLocalPort())
    }

    @Test
    fun `isActive returns true when session is connected`() {
        // Given
        val config = SSHTunnelConfig(
            enabled = true,
            host = "bastion.example.com",
            port = 22,
            username = "sshuser",
            authMethod = SSHAuthMethod.PASSWORD,
            password = "password"
        )
        
        val manager = SSHTunnelManager(config, context)
        
        every { mockSession.isConnected } returns true
        manager.connect("db.internal.com", 3306)
        
        // When
        val isActive = manager.isActive()
        
        // Then
        assertTrue(isActive)
    }

    @Test
    fun `isActive returns false when session is disconnected`() {
        // Given
        val config = SSHTunnelConfig(
            enabled = true,
            host = "bastion.example.com",
            port = 22,
            username = "sshuser",
            authMethod = SSHAuthMethod.PASSWORD,
            password = "password"
        )
        
        val manager = SSHTunnelManager(config, context)
        
        every { mockSession.isConnected } returns false
        
        // When
        val isActive = manager.isActive()
        
        // Then
        assertFalse(isActive)
    }

    @Test
    fun `getLocalPort returns port after successful connection`() {
        // Given
        val config = SSHTunnelConfig(
            enabled = true,
            host = "bastion.example.com",
            port = 22,
            username = "sshuser",
            authMethod = SSHAuthMethod.PASSWORD,
            password = "password"
        )
        
        val manager = SSHTunnelManager(config, context)
        
        every { mockSession.isConnected } returns true
        
        // When
        val localPort = manager.connect("db.internal.com", 3306)
        val retrievedPort = manager.getLocalPort()
        
        // Then
        assertEquals(localPort, retrievedPort)
        assertNotNull(retrievedPort)
    }

    @Test
    fun `getLocalPort returns null before connection`() {
        // Given
        val config = SSHTunnelConfig(
            enabled = true,
            host = "bastion.example.com",
            port = 22,
            username = "sshuser",
            authMethod = SSHAuthMethod.PASSWORD,
            password = "password"
        )
        
        val manager = SSHTunnelManager(config, context)
        
        // When
        val localPort = manager.getLocalPort()
        
        // Then
        assertNull(localPort)
    }

    @Test
    fun `ensureConnected reconnects when tunnel is dropped`() {
        // Given
        val config = SSHTunnelConfig(
            enabled = true,
            host = "bastion.example.com",
            port = 22,
            username = "sshuser",
            authMethod = SSHAuthMethod.PASSWORD,
            password = "password"
        )
        
        val manager = SSHTunnelManager(config, context)
        
        // First connection
        every { mockSession.isConnected } returns true
        manager.connect("db.internal.com", 3306)
        
        // Tunnel drops
        every { mockSession.isConnected } returns false
        
        // When - ensureConnected should reconnect
        every { mockSession.isConnected } returns true andThen true
        val result = manager.ensureConnected("db.internal.com", 3306)
        
        // Then
        assertTrue(result)
        assertTrue(manager.isActive())
    }

    @Test
    fun `ensureConnected returns false when reconnect fails`() {
        // Given
        val config = SSHTunnelConfig(
            enabled = true,
            host = "bastion.example.com",
            port = 22,
            username = "sshuser",
            authMethod = SSHAuthMethod.PASSWORD,
            password = "password"
        )
        
        val manager = SSHTunnelManager(config, context)
        
        // Tunnel is down
        every { mockSession.isConnected } returns false
        
        // Reconnect fails
        every { mockSession.connect() } throws JSchException("Connection refused")
        
        // When
        val result = manager.ensureConnected("db.internal.com", 3306)
        
        // Then
        assertFalse(result)
    }

    @Test
    fun `connect with private key auth uses addIdentity`() {
        // Given
        val keyUri = "content://com.android.providers.downloads/key.pem"
        val config = SSHTunnelConfig(
            enabled = true,
            host = "bastion.example.com",
            port = 22,
            username = "sshuser",
            authMethod = SSHAuthMethod.PRIVATE_KEY,
            password = "",
            privateKeyUri = keyUri
        )
        
        // Mock SSHKeyReader
        mockkObject(SSHKeyReader)
        every { SSHKeyReader.readPrivateKey(any(), any()) } returns "mock-key-bytes".toByteArray()
        
        val manager = SSHTunnelManager(config, context)
        
        every { mockSession.isConnected } returns true
        
        // When
        manager.connect("db.internal.com", 3306)
        
        // Then
        verify { SSHKeyReader.readPrivateKey(context, Uri.parse(keyUri)) }
        unmockkObject(SSHKeyReader)
    }

    @Test(expected = SSHTunnelException.InvalidKey::class)
    fun `connect with encrypted private key throws InvalidKey`() {
        // Given
        val keyUri = "content://com.android.providers.downloads/encrypted-key.pem"
        val config = SSHTunnelConfig(
            enabled = true,
            host = "bastion.example.com",
            port = 22,
            username = "sshuser",
            authMethod = SSHAuthMethod.PRIVATE_KEY,
            password = "",
            privateKeyUri = keyUri
        )
        
        // Mock SSHKeyReader throwing encrypted key exception
        mockkObject(SSHKeyReader)
        every { SSHKeyReader.readPrivateKey(any(), any()) } throws 
            IllegalStateException("Encrypted SSH keys not supported")
        
        val manager = SSHTunnelManager(config, context)
        
        // When
        manager.connect("db.internal.com", 3306)
        
        // Then - exception thrown
        unmockkObject(SSHKeyReader)
    }
}
