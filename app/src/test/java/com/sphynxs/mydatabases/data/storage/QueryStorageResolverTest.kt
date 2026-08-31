package com.sphynxs.mydatabases.data.storage

import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.sphynxs.mydatabases.domain.models.RootResolution
import com.sphynxs.mydatabases.domain.repositories.SettingsRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * TDD tests for `QueryStorageResolver` (change `query-files-storage`, Phase 3).
 *
 * The correctness-critical component: decides whether to use the app-private root or the
 * user-configured SAF tree, and detects/falls back on permission loss. `QueryStorageRootProvider`
 * is mocked so this stays a pure logic test — no real `DocumentFile`/`Context` I/O.
 *
 * @author sdd-apply (Strict TDD)
 * @date 2026-08-05
 */
class QueryStorageResolverTest {

    private lateinit var settingsRepository: SettingsRepository
    private lateinit var provider: QueryStorageRootProvider
    private lateinit var resolver: QueryStorageResolver

    private val privateRoot = mockk<DocumentFile>(relaxed = true) {
        every { exists() } returns true
        every { canWrite() } returns true
    }

    @Before
    fun setup() {
        settingsRepository = mockk()
        provider = mockk()
        resolver = QueryStorageResolver(settingsRepository, provider)
        every { provider.privateRoot() } returns privateRoot
    }

    @Test
    fun `null preference resolves to the private root`() = runTest {
        every { settingsRepository.observeQueryStorageTreeUri() } returns flowOf(null)

        val result = resolver.resolveRoot()

        assertTrue(result is RootResolution.Resolved)
        assertTrue((result as RootResolution.Resolved).root === privateRoot)
    }

    @Test
    fun `valid persisted SAF tree resolves to the SAF root`() = runTest {
        val treeUri = mockk<Uri>(relaxed = true)
        val safRoot = mockk<DocumentFile>(relaxed = true) {
            every { exists() } returns true
            every { canWrite() } returns true
        }
        every { settingsRepository.observeQueryStorageTreeUri() } returns flowOf(treeUri)
        every { provider.safRoot(treeUri) } returns safRoot

        val result = resolver.resolveRoot()

        assertTrue(result is RootResolution.Resolved)
        assertTrue((result as RootResolution.Resolved).root === safRoot)
    }

    @Test
    fun `SAF tree returning null from the provider falls back to private root`() = runTest {
        val treeUri = mockk<Uri>(relaxed = true)
        every { settingsRepository.observeQueryStorageTreeUri() } returns flowOf(treeUri)
        every { provider.safRoot(treeUri) } returns null

        val result = resolver.resolveRoot()

        assertTrue(result is RootResolution.Fallback)
        assertTrue((result as RootResolution.Fallback).root === privateRoot)
    }

    @Test
    fun `SAF tree that no longer exists falls back to private root`() = runTest {
        val treeUri = mockk<Uri>(relaxed = true)
        val staleSafRoot = mockk<DocumentFile>(relaxed = true) {
            every { exists() } returns false
        }
        every { settingsRepository.observeQueryStorageTreeUri() } returns flowOf(treeUri)
        every { provider.safRoot(treeUri) } returns staleSafRoot

        val result = resolver.resolveRoot()

        assertTrue(result is RootResolution.Fallback)
    }

    @Test
    fun `SAF tree that is no longer writable falls back to private root`() = runTest {
        val treeUri = mockk<Uri>(relaxed = true)
        val readOnlySafRoot = mockk<DocumentFile>(relaxed = true) {
            every { exists() } returns true
            every { canWrite() } returns false
        }
        every { settingsRepository.observeQueryStorageTreeUri() } returns flowOf(treeUri)
        every { provider.safRoot(treeUri) } returns readOnlySafRoot

        val result = resolver.resolveRoot()

        assertTrue(result is RootResolution.Fallback)
    }

    @Test
    fun `every call re-checks the condition, not cached after first resolve`() = runTest {
        // First call: valid SAF tree.
        val treeUri = mockk<Uri>(relaxed = true)
        val safRoot = mockk<DocumentFile>(relaxed = true) {
            every { exists() } returns true
            every { canWrite() } returns true
        }
        every { settingsRepository.observeQueryStorageTreeUri() } returns flowOf(treeUri)
        every { provider.safRoot(treeUri) } returns safRoot
        val first = resolver.resolveRoot()
        assertTrue(first is RootResolution.Resolved)

        // Second call: same resolver instance, but the tree is now unavailable (revoked mid-session).
        every { provider.safRoot(treeUri) } returns null
        val second = resolver.resolveRoot()
        assertTrue(second is RootResolution.Fallback)
    }
}
