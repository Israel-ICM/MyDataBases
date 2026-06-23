package com.sphynxs.mydatabases.domain.usecases

import com.sphynxs.mydatabases.core.database.models.DatabaseError
import com.sphynxs.mydatabases.core.database.repository.DatabaseRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for CreateDatabaseUseCase.
 *
 * Tests cover:
 * - SQL composition with name only
 * - SQL composition with charset only
 * - SQL composition with collation only
 * - SQL composition with both charset and collation
 * - Identifier validation (backtick, semicolon, space, exceeding 64 chars)
 * - Result propagation (success and failure)
 *
 * @author sdd-apply (Strict TDD)
 * @date 2026-06-23
 */
class CreateDatabaseUseCaseTest {
    
    private lateinit var repository: DatabaseRepository
    private lateinit var useCase: CreateDatabaseUseCase
    
    @Before
    fun setup() {
        repository = mockk()
        useCase = CreateDatabaseUseCase(repository)
    }
    
    // ========== SQL Composition Tests ==========
    
    @Test
    fun `invoke with name only produces CREATE DATABASE with backticks`() = runTest {
        // GIVEN
        val name = "analytics_2026"
        val expectedSQL = "CREATE DATABASE `analytics_2026`"
        coEvery { repository.executeUpdate(expectedSQL, emptyList()) } returns Result.success(1)
        
        // WHEN
        val result = useCase(name)
        
        // THEN
        assertTrue(result.isSuccess)
        assertEquals(Unit, result.getOrNull())
        coVerify(exactly = 1) { repository.executeUpdate(expectedSQL, emptyList()) }
    }
    
    @Test
    fun `invoke with charset only includes CHARACTER SET clause`() = runTest {
        // GIVEN
        val name = "analytics_2026"
        val charset = "utf8mb4"
        val expectedSQL = "CREATE DATABASE `analytics_2026` CHARACTER SET `utf8mb4`"
        coEvery { repository.executeUpdate(expectedSQL, emptyList()) } returns Result.success(1)
        
        // WHEN
        val result = useCase(name, charset = charset)
        
        // THEN
        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { repository.executeUpdate(expectedSQL, emptyList()) }
    }
    
    @Test
    fun `invoke with collation only includes COLLATE clause`() = runTest {
        // GIVEN
        val name = "analytics_2026"
        val collation = "utf8mb4_unicode_ci"
        val expectedSQL = "CREATE DATABASE `analytics_2026` COLLATE `utf8mb4_unicode_ci`"
        coEvery { repository.executeUpdate(expectedSQL, emptyList()) } returns Result.success(1)
        
        // WHEN
        val result = useCase(name, collation = collation)
        
        // THEN
        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { repository.executeUpdate(expectedSQL, emptyList()) }
    }
    
    @Test
    fun `invoke with both charset and collation includes both clauses in correct order`() = runTest {
        // GIVEN
        val name = "analytics_2026"
        val charset = "utf8mb4"
        val collation = "utf8mb4_unicode_ci"
        val expectedSQL = "CREATE DATABASE `analytics_2026` CHARACTER SET `utf8mb4` COLLATE `utf8mb4_unicode_ci`"
        coEvery { repository.executeUpdate(expectedSQL, emptyList()) } returns Result.success(1)
        
        // WHEN
        val result = useCase(name, charset = charset, collation = collation)
        
        // THEN
        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { repository.executeUpdate(expectedSQL, emptyList()) }
    }
    
    @Test
    fun `invoke trims whitespace from all parameters`() = runTest {
        // GIVEN
        val name = "  analytics_2026  "
        val charset = "  utf8mb4  "
        val collation = "  utf8mb4_unicode_ci  "
        val expectedSQL = "CREATE DATABASE `analytics_2026` CHARACTER SET `utf8mb4` COLLATE `utf8mb4_unicode_ci`"
        coEvery { repository.executeUpdate(expectedSQL, emptyList()) } returns Result.success(1)
        
        // WHEN
        val result = useCase(name, charset = charset, collation = collation)
        
        // THEN
        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { repository.executeUpdate(expectedSQL, emptyList()) }
    }
    
    @Test
    fun `invoke treats blank charset as null and omits CHARACTER SET clause`() = runTest {
        // GIVEN
        val name = "analytics_2026"
        val charset = "   "
        val expectedSQL = "CREATE DATABASE `analytics_2026`"
        coEvery { repository.executeUpdate(expectedSQL, emptyList()) } returns Result.success(1)
        
        // WHEN
        val result = useCase(name, charset = charset)
        
        // THEN
        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { repository.executeUpdate(expectedSQL, emptyList()) }
    }
    
    @Test
    fun `invoke treats blank collation as null and omits COLLATE clause`() = runTest {
        // GIVEN
        val name = "analytics_2026"
        val collation = ""
        val expectedSQL = "CREATE DATABASE `analytics_2026`"
        coEvery { repository.executeUpdate(expectedSQL, emptyList()) } returns Result.success(1)
        
        // WHEN
        val result = useCase(name, collation = collation)
        
        // THEN
        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { repository.executeUpdate(expectedSQL, emptyList()) }
    }
    
    // ========== Identifier Validation Tests ==========
    
    @Test
    fun `invoke rejects name with backtick before calling repository`() = runTest {
        // GIVEN
        val invalidName = "my`db"
        
        // WHEN
        val result = useCase(invalidName)
        
        // THEN
        assertTrue(result.isFailure)
        val error = result.exceptionOrNull()
        assertTrue(error is DatabaseError.InvalidConfiguration)
        coVerify(exactly = 0) { repository.executeUpdate(any(), any()) }
    }
    
    @Test
    fun `invoke rejects name with semicolon (SQL injection attempt) before calling repository`() = runTest {
        // GIVEN
        val invalidName = "analytics; DROP DATABASE prod"
        
        // WHEN
        val result = useCase(invalidName)
        
        // THEN
        assertTrue(result.isFailure)
        val error = result.exceptionOrNull()
        assertTrue(error is DatabaseError.InvalidConfiguration)
        coVerify(exactly = 0) { repository.executeUpdate(any(), any()) }
    }
    
    @Test
    fun `invoke rejects charset with space before calling repository`() = runTest {
        // GIVEN
        val name = "analytics_2026"
        val invalidCharset = "utf8 mb4"
        
        // WHEN
        val result = useCase(name, charset = invalidCharset)
        
        // THEN
        assertTrue(result.isFailure)
        val error = result.exceptionOrNull()
        assertTrue(error is DatabaseError.InvalidConfiguration)
        coVerify(exactly = 0) { repository.executeUpdate(any(), any()) }
    }
    
    @Test
    fun `invoke rejects collation with quote (SQL injection attempt) before calling repository`() = runTest {
        // GIVEN
        val name = "analytics_2026"
        val invalidCollation = "utf8mb4_unicode_ci' OR '1'='1"
        
        // WHEN
        val result = useCase(name, collation = invalidCollation)
        
        // THEN
        assertTrue(result.isFailure)
        val error = result.exceptionOrNull()
        assertTrue(error is DatabaseError.InvalidConfiguration)
        coVerify(exactly = 0) { repository.executeUpdate(any(), any()) }
    }
    
    @Test
    fun `invoke rejects name exceeding 64 characters before calling repository`() = runTest {
        // GIVEN
        val invalidName = "a".repeat(65)
        
        // WHEN
        val result = useCase(invalidName)
        
        // THEN
        assertTrue(result.isFailure)
        val error = result.exceptionOrNull()
        assertTrue(error is DatabaseError.InvalidConfiguration)
        coVerify(exactly = 0) { repository.executeUpdate(any(), any()) }
    }
    
    @Test
    fun `invoke accepts name with exactly 64 characters`() = runTest {
        // GIVEN
        val validName = "a".repeat(64)
        val expectedSQL = "CREATE DATABASE `$validName`"
        coEvery { repository.executeUpdate(expectedSQL, emptyList()) } returns Result.success(1)
        
        // WHEN
        val result = useCase(validName)
        
        // THEN
        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { repository.executeUpdate(expectedSQL, emptyList()) }
    }
    
    // ========== Result Propagation Tests ==========
    
    @Test
    fun `invoke propagates repository success as Result success Unit`() = runTest {
        // GIVEN
        val name = "analytics_2026"
        val expectedSQL = "CREATE DATABASE `analytics_2026`"
        coEvery { repository.executeUpdate(expectedSQL, emptyList()) } returns Result.success(1)
        
        // WHEN
        val result = useCase(name)
        
        // THEN
        assertTrue(result.isSuccess)
        assertEquals(Unit, result.getOrNull())
    }
    
    @Test
    fun `invoke propagates repository failure unchanged`() = runTest {
        // GIVEN
        val name = "analytics_2026"
        val expectedSQL = "CREATE DATABASE `analytics_2026`"
        val databaseError = DatabaseError.QueryExecutionFailed(
            query = expectedSQL,
            reason = "Can't create database 'analytics_2026'; database exists"
        )
        coEvery { repository.executeUpdate(expectedSQL, emptyList()) } returns Result.failure(databaseError)
        
        // WHEN
        val result = useCase(name)
        
        // THEN
        assertTrue(result.isFailure)
        assertEquals(databaseError, result.exceptionOrNull())
    }
    
    @Test
    fun `invoke propagates ConnectionFailed unchanged`() = runTest {
        // GIVEN
        val name = "analytics_2026"
        val expectedSQL = "CREATE DATABASE `analytics_2026`"
        val connectionError = DatabaseError.ConnectionFailed("No conectado")
        coEvery { repository.executeUpdate(expectedSQL, emptyList()) } returns Result.failure(connectionError)
        
        // WHEN
        val result = useCase(name)
        
        // THEN
        assertTrue(result.isFailure)
        assertEquals(connectionError, result.exceptionOrNull())
    }
}
