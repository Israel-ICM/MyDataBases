package com.sphynxs.mydatabases.localization

import org.junit.Test
import org.w3c.dom.Document
import org.w3c.dom.Element
import javax.xml.parsers.DocumentBuilderFactory
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Assert.fail

/**
 * Test de paridad de localización entre inglés y español.
 *
 * Verifica que todos los strings definidos en values/strings.xml
 * existan también en values-es/strings.xml.
 *
 * @author israel-icm
 * @date 2026-06-12
 */
class LocalizationParityTest {
    
    /**
     * Verifica que todos los strings en inglés tengan traducción al español.
     */
    @Test
    fun allEnglishStrings_haveSpanishTranslation() {
        // GIVEN: archivos de recursos (path relativo desde módulo :app)
        val enStringsFile = File("src/main/res/values/strings.xml")
        val esStringsFile = File("src/main/res/values-es/strings.xml")
        
        // Verificar que el archivo en inglés existe
        assertTrue("English strings file must exist", enStringsFile.exists())
        
        // Verificar que el archivo en español existe
        assertTrue("Spanish strings file must exist", esStringsFile.exists())
        
        // WHEN: parseamos ambos archivos
        val enStrings = parseStringKeys(enStringsFile)
        val esStrings = parseStringKeys(esStringsFile)
        
        // THEN: todos los strings en inglés deben existir en español
        val missingInSpanish = enStrings - esStrings
        
        if (missingInSpanish.isNotEmpty()) {
            fail("Missing Spanish translations for keys: ${missingInSpanish.joinToString(", ")}")
        }
    }
    
    /**
     * Parsea un archivo strings.xml y devuelve el conjunto de keys.
     */
    private fun parseStringKeys(file: File): Set<String> {
        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()
        val document: Document = builder.parse(file)
        
        val stringElements = document.getElementsByTagName("string")
        val keys = mutableSetOf<String>()
        
        for (i in 0 until stringElements.length) {
            val element = stringElements.item(i) as Element
            val name = element.getAttribute("name")
            if (name.isNotEmpty()) {
                keys.add(name)
            }
        }
        
        return keys
    }
}
