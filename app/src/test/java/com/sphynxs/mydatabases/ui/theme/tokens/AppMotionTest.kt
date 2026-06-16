package com.sphynxs.mydatabases.ui.theme.tokens

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests unitarios para AppMotion.
 *
 * Verifica duraciones, easings y la función durationOrInstant() que
 * respeta reduced motion.
 *
 * @author israel-icm (TDD RED → TRIANGULATE)
 * @date 2026-06-15
 */
class AppMotionTest {

    @Test
    fun `durationOrInstant devuelve 0 cuando reduced=true`() {
        val motion = AppMotion()
        assertEquals(0, motion.durationOrInstant(300, reduced = true))
    }
    
    @Test
    fun `durationOrInstant devuelve base cuando reduced=false`() {
        val motion = AppMotion()
        assertEquals(300, motion.durationOrInstant(300, reduced = false))
    }
    
    @Test
    fun `durationOrInstant con fast retorna 150 cuando reduced=false`() {
        val motion = AppMotion()
        assertEquals(150, motion.durationOrInstant(motion.fast, reduced = false))
    }
    
    @Test
    fun `durationOrInstant con slow retorna 0 cuando reduced=true`() {
        val motion = AppMotion()
        assertEquals(0, motion.durationOrInstant(motion.slow, reduced = true))
    }
}
