package com.sphynxs.mydatabases

import android.content.res.Configuration
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

/**
 * Verifies workspace carousel content-description strings exist and are genuinely
 * translated for en + es (Scenario 14, `openspec/changes/workspace-card-carousel/spec.md`).
 *
 * TDD note: written FIRST against `R.string.workspace_carousel_button` /
 * `R.string.workspace_carousel_close_card`, which did not exist before this apply batch
 * (task 4.4/4.5). Execution deferred to the maintainer per explicit session instruction —
 * see apply-progress TDD Cycle Evidence table.
 */
@RunWith(AndroidJUnit4::class)
class StringsResourceTest {

    private fun stringFor(locale: Locale, resId: Int, vararg formatArgs: Any): String {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(locale)
        val localizedContext = context.createConfigurationContext(configuration)
        return if (formatArgs.isEmpty()) {
            localizedContext.resources.getString(resId)
        } else {
            localizedContext.resources.getString(resId, *formatArgs)
        }
    }

    /**
     * GIVEN the device locale is `en` or `es` THEN `workspace_carousel_button` and
     * `workspace_carousel_close_card` MUST resolve to distinct, non-blank, genuinely
     * translated strings matching design.md's i18n table.
     */
    @Test
    fun workspace_carousel_strings_existInEnAndEs() {
        val enButton = stringFor(Locale.ENGLISH, R.string.workspace_carousel_button)
        val esButton = stringFor(Locale("es"), R.string.workspace_carousel_button)

        assertTrue("en button string must not be blank", enButton.isNotBlank())
        assertTrue("es button string must not be blank", esButton.isNotBlank())
        assertTrue("en and es strings must be genuinely translated", enButton != esButton)
        assertEquals("Show all open cards", enButton)
        assertEquals("Ver todas las tarjetas abiertas", esButton)

        val enClose = stringFor(Locale.ENGLISH, R.string.workspace_carousel_close_card, "users")
        val esClose = stringFor(Locale("es"), R.string.workspace_carousel_close_card, "users")

        assertEquals("Close users", enClose)
        assertEquals("Cerrar users", esClose)
    }
}
