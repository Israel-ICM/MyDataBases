package com.sphynxs.mydatabases.ui.screens.databases

import com.sphynxs.mydatabases.core.database.models.DatabaseError
import com.sphynxs.mydatabases.domain.usecases.CreateDatabaseUseCase
import com.sphynxs.mydatabases.domain.usecases.GetCharacterSetsUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for AddDatabaseViewModel.createDatabase function and submit state machine.
 *
 * Tests cover:
 * - Initial state is Idle
 * - Calling createDatabase transitions to Submitting
 * - Use case success transitions to Success
 * - Error mapping (database exists, Access denied, ConnectionFailed, InvalidConfiguration, generic)
 * - resetSubmitState returns to Idle
 *
 * @author sdd-apply (Strict TDD)
 * @date 2026-06-23
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AddDatabaseViewModelTest {
    
    private lateinit var getCharacterSetsUseCase: GetCharacterSetsUseCase
    private lateinit var createDatabaseUseCase: CreateDatabaseUseCase
    private lateinit var viewModel: AddDatabaseViewModel
    
    private val testDispatcher = StandardTestDispatcher()
    
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        getCharacterSetsUseCase = mockk(relaxed = true)
        createDatabaseUseCase = mockk()
        viewModel = AddDatabaseViewModel(getCharacterSetsUseCase, createDatabaseUseCase)
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    // ========== Initial State Tests ==========
    
    @Test
    fun `submitState initial value is Idle`() {
        // THEN
        assertEquals(CreateDatabaseState.Idle, viewModel.submitState.value)
    }
    
    // ========== State Transition Tests ==========
    
    @Test
    fun `createDatabase transitions to Submitting before use case completes`() = runTest {
        // GIVEN
        coEvery { createDatabaseUseCase("analytics_2026") } coAnswers {
            // Suspend indefinitely so we can observe Submitting state
            kotlinx.coroutines.delay(Long.MAX_VALUE)
            Result.success(Unit)
        }
        
        // WHEN
        viewModel.createDatabase("analytics_2026", null, null)
        
        // THEN (immediately after call, before use case completes)
        assertEquals(CreateDatabaseState.Submitting, viewModel.submitState.value)
    }
    
    @Test
    fun `createDatabase transitions to Success when use case succeeds`() = runTest {
        // GIVEN
        coEvery { createDatabaseUseCase("analytics_2026", null, null) } returns Result.success(Unit)
        
        // WHEN
        viewModel.createDatabase("analytics_2026", null, null)
        advanceUntilIdle()
        
        // THEN
        assertEquals(CreateDatabaseState.Success, viewModel.submitState.value)
        coVerify(exactly = 1) { createDatabaseUseCase("analytics_2026", null, null) }
    }
    
    // ========== Error Mapping Tests ==========
    
    @Test
    fun `createDatabase maps database exists error to specific message`() = runTest {
        // GIVEN
        val error = DatabaseError.QueryExecutionFailed(
            query = "CREATE DATABASE `analytics_2026`",
            reason = "Can't create database 'analytics_2026'; database exists (errno: 1007)"
        )
        coEvery { createDatabaseUseCase("analytics_2026", null, null) } returns Result.failure(error)
        
        // WHEN
        viewModel.createDatabase("analytics_2026", null, null)
        advanceUntilIdle()
        
        // THEN
        val state = viewModel.submitState.value
        assertTrue(state is CreateDatabaseState.Error)
        assertTrue((state as CreateDatabaseState.Error).message.contains("already exists") ||
                   state.message.contains("existe"))
    }
    
    @Test
    fun `createDatabase maps Access denied error to permission message`() = runTest {
        // GIVEN
        val error = DatabaseError.QueryExecutionFailed(
            query = "CREATE DATABASE `analytics_2026`",
            reason = "Access denied for user 'readonly'@'localhost' (using password: YES)"
        )
        coEvery { createDatabaseUseCase("analytics_2026", null, null) } returns Result.failure(error)
        
        // WHEN
        viewModel.createDatabase("analytics_2026", null, null)
        advanceUntilIdle()
        
        // THEN
        val state = viewModel.submitState.value
        assertTrue(state is CreateDatabaseState.Error)
        assertTrue((state as CreateDatabaseState.Error).message.contains("permission") ||
                   state.message.contains("permisos"))
    }
    
    @Test
    fun `createDatabase maps command denied error to permission message`() = runTest {
        // GIVEN
        val error = DatabaseError.QueryExecutionFailed(
            query = "CREATE DATABASE `analytics_2026`",
            reason = "CREATE command denied to user 'readonly'@'localhost' for database 'analytics_2026'"
        )
        coEvery { createDatabaseUseCase("analytics_2026", null, null) } returns Result.failure(error)
        
        // WHEN
        viewModel.createDatabase("analytics_2026", null, null)
        advanceUntilIdle()
        
        // THEN
        val state = viewModel.submitState.value
        assertTrue(state is CreateDatabaseState.Error)
        assertTrue((state as CreateDatabaseState.Error).message.contains("permission") ||
                   state.message.contains("permisos"))
    }
    
    @Test
    fun `createDatabase maps ConnectionFailed to connection-lost message`() = runTest {
        // GIVEN
        val error = DatabaseError.ConnectionFailed("No conectado")
        coEvery { createDatabaseUseCase("analytics_2026", null, null) } returns Result.failure(error)
        
        // WHEN
        viewModel.createDatabase("analytics_2026", null, null)
        advanceUntilIdle()
        
        // THEN
        val state = viewModel.submitState.value
        assertTrue(state is CreateDatabaseState.Error)
        assertTrue((state as CreateDatabaseState.Error).message.contains("connection") ||
                   state.message.contains("conexi"))
    }
    
    @Test
    fun `createDatabase maps InvalidConfiguration to invalid-name message`() = runTest {
        // GIVEN
        val error = DatabaseError.InvalidConfiguration(
            field = "database_name",
            reason = "Must match ^[A-Za-z0-9_]{1,64}$"
        )
        coEvery { createDatabaseUseCase("my`db", null, null) } returns Result.failure(error)
        
        // WHEN
        viewModel.createDatabase("my`db", null, null)
        advanceUntilIdle()
        
        // THEN
        val state = viewModel.submitState.value
        assertTrue(state is CreateDatabaseState.Error)
        assertTrue((state as CreateDatabaseState.Error).message.contains("invalid") ||
                   state.message.contains("inválid"))
    }
    
    @Test
    fun `createDatabase maps generic QueryExecutionFailed to generic failure message`() = runTest {
        // GIVEN
        val error = DatabaseError.QueryExecutionFailed(
            query = "CREATE DATABASE `analytics_2026`",
            reason = "Unknown MySQL server host 'badhost' (0)"
        )
        coEvery { createDatabaseUseCase("analytics_2026", null, null) } returns Result.failure(error)
        
        // WHEN
        viewModel.createDatabase("analytics_2026", null, null)
        advanceUntilIdle()
        
        // THEN
        val state = viewModel.submitState.value
        assertTrue(state is CreateDatabaseState.Error)
        assertTrue((state as CreateDatabaseState.Error).message.contains("Could not") ||
                   state.message.contains("pudo"))
    }
    
    // ========== Reset State Tests ==========
    
    @Test
    fun `resetSubmitState returns state to Idle from Success`() = runTest {
        // GIVEN
        coEvery { createDatabaseUseCase("analytics_2026", null, null) } returns Result.success(Unit)
        viewModel.createDatabase("analytics_2026", null, null)
        advanceUntilIdle()
        assertEquals(CreateDatabaseState.Success, viewModel.submitState.value)
        
        // WHEN
        viewModel.resetSubmitState()
        
        // THEN
        assertEquals(CreateDatabaseState.Idle, viewModel.submitState.value)
    }
    
    @Test
    fun `resetSubmitState returns state to Idle from Error`() = runTest {
        // GIVEN
        val error = DatabaseError.ConnectionFailed("No conectado")
        coEvery { createDatabaseUseCase("analytics_2026", null, null) } returns Result.failure(error)
        viewModel.createDatabase("analytics_2026", null, null)
        advanceUntilIdle()
        assertTrue(viewModel.submitState.value is CreateDatabaseState.Error)
        
        // WHEN
        viewModel.resetSubmitState()
        
        // THEN
        assertEquals(CreateDatabaseState.Idle, viewModel.submitState.value)
    }
}
