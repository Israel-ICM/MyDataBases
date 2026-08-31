package com.sphynxs.mydatabases.data.repositories

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sphynxs.mydatabases.core.database.engine.DatabaseType
import com.sphynxs.mydatabases.data.storage.DefaultQueryStorageRootProvider
import com.sphynxs.mydatabases.data.storage.QueryStorageResolver
import com.sphynxs.mydatabases.domain.repositories.SettingsRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Real `DocumentFile`/`ContentResolver` I/O tests for `QueryFileStoreImpl` (change
 * `query-files-storage`, task 4.7).
 *
 * **Populated but NOT executed in this session** — no `./gradlew connectedAndroidTest` run
 * (project HARD RULE forbids it) and no emulator/device is available (`adb devices` returned
 * empty). This class exists so a maintainer with a real device can run it directly; it is
 * grounded in the exact `QueryFileStoreImpl` contract, not a guess.
 *
 * Covers both storage models uniformly (file:// via the app-private root, content:// via a SAF
 * tree) since [DocumentFile] is meant to unify them behind one `Uri`-based API.
 *
 * @author sdd-apply (Strict TDD)
 * @date 2026-08-05
 */
@RunWith(AndroidJUnit4::class)
class QueryFileStoreImplInstrumentedTest {

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

    /** Uses the real app-private root (`getExternalFilesDir`) — no SAF permission needed. */
    private fun privateStore(): QueryFileStoreImpl {
        val settingsRepository = mockk<SettingsRepository>()
        coEvery { settingsRepository.observeQueryStorageTreeUri() } returns flowOf(null)
        val provider = DefaultQueryStorageRootProvider(context)
        val resolver = QueryStorageResolver(settingsRepository, provider)
        return QueryFileStoreImpl(resolver, context)
    }

    @Test
    fun write_then_list_finds_the_file_under_the_private_engine_folder() = runBlocking {
        val store = privateStore()
        val fileName = "instrumented_${System.currentTimeMillis()}.sql"

        val writeResult = store.write(DatabaseType.MYSQL, fileName, "SELECT 1;")
        assertTrue(writeResult.isSuccess)

        val listResult = store.list(DatabaseType.MYSQL)
        assertTrue(listResult.isSuccess)
        assertTrue(listResult.getOrNull()!!.any { it.name == fileName })

        // Cleanup
        store.delete(writeResult.getOrNull()!!)
    }

    @Test
    fun write_then_read_round_trips_the_exact_content() = runBlocking {
        val store = privateStore()
        val fileName = "roundtrip_${System.currentTimeMillis()}.sql"
        val content = "SELECT * FROM t WHERE id = 1;"

        val uri = store.write(DatabaseType.POSTGRESQL, fileName, content).getOrThrow()
        val read = store.read(uri).getOrThrow()

        assertEquals(content, read)
        store.delete(uri)
    }

    @Test
    fun delete_removes_the_file_so_it_no_longer_appears_in_list() = runBlocking {
        val store = privateStore()
        val fileName = "to_delete_${System.currentTimeMillis()}.sql"
        val uri = store.write(DatabaseType.MARIADB, fileName, "SELECT 1;").getOrThrow()

        val deleteResult = store.delete(uri)
        assertTrue(deleteResult.isSuccess)
        assertTrue(deleteResult.getOrNull() == true)

        val listResult = store.list(DatabaseType.MARIADB)
        assertTrue(listResult.getOrNull()!!.none { it.name == fileName })
    }

    @Test
    fun engine_isolation_holds_with_a_real_filesystem() = runBlocking {
        val store = privateStore()
        val mysqlFile = "engine_isolation_${System.currentTimeMillis()}.sql"

        val uri = store.write(DatabaseType.MYSQL, mysqlFile, "SELECT 1;").getOrThrow()
        val sqliteList = store.list(DatabaseType.SQLITE)

        assertTrue(sqliteList.getOrNull()!!.none { it.name == mysqlFile })
        store.delete(uri)
    }

    // NOTE: a SAF-tree ("content://") variant of these same 4 tests requires an actual
    // OpenDocumentTree grant obtained interactively on a device — cannot be automated fully;
    // a maintainer running these manually should grant a tree via the Settings UI first, then
    // add an equivalent `safStore()` factory pointing at that granted tree Uri.
}
