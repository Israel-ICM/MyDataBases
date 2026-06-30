package com.sphynxs.mydatabases.core.database.models

/**
 * SSH tunnel configuration for secure database connections through bastion/jump hosts.
 *
 * SSH tunneling (port forwarding) allows connections to databases behind firewalls
 * or in private networks accessible only through intermediate SSH servers.
 *
 * Common use case:
 * ```
 * Android App → SSH Tunnel → Bastion Host → Database Server
 * ```
 *
 * This configuration is complementary to SSL/TLS (not exclusive):
 * - SSH secures the transport layer (encrypted tunnel)
 * - SSL secures the connection layer (JDBC encrypted connection)
 *
 * Layered security example:
 * ```
 * App → SSH tunnel → Bastion → SSL connection → MySQL Server
 * ```
 *
 * @property enabled Whether SSH tunnel is enabled for this connection
 * @property host SSH server hostname or IP address (bastion/jump host)
 * @property port SSH server port (default: 22)
 * @property username SSH authentication username
 * @property authMethod Authentication method (password or private key)
 * @property password SSH password (required if authMethod = PASSWORD, stored encrypted)
 * @property privateKeyUri Android URI to private key file (required if authMethod = PRIVATE_KEY)
 *
 * @author israel-icm
 * @date 2026-06-30
 */
data class SSHTunnelConfig(
    val enabled: Boolean = false,
    val host: String = "",
    val port: Int = 22,
    val username: String = "",
    val authMethod: SSHAuthMethod = SSHAuthMethod.PASSWORD,
    val password: String = "",
    val privateKeyUri: String? = null
)

/**
 * SSH authentication methods supported by MyDataBases.
 *
 * @property PASSWORD Username + password authentication
 * @property PRIVATE_KEY Public key authentication with private key file
 *
 * Note: Private keys with passphrase (encrypted keys) are NOT supported in v1.0.
 * Use unencrypted private keys or password authentication instead.
 */
enum class SSHAuthMethod {
    /** Username + password authentication */
    PASSWORD,
    
    /** Public key authentication with private key file (PEM/OpenSSH format) */
    PRIVATE_KEY
}
