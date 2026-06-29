package com.sphynxs.mydatabases.domain.editor

import androidx.compose.ui.text.TextRange

/**
 * Pure JVM find/replace engine (no Android deps).
 *
 * Task 5.1 — Domain layer for FR-1 through FR-15.
 *
 * Handles:
 * - Regex-based text search with match case / whole word / regex mode
 * - Match navigation (next/previous with wrap)
 * - Replace one / replace all operations
 * - Match highlighting via TextRange list
 *
 * @author israel-icm (SDD apply phase)
 * @date 2026-06-29
 */
object FindReplaceEngine {

    /**
     * Find all matches in text.
     *
     * @param text Source text to search
     * @param query Search query
     * @param matchCase Case-sensitive search
     * @param wholeWord Match whole words only
     * @param useRegex Treat query as regex pattern
     * @return List of match ranges, or empty if no matches
     */
    fun findAllMatches(
        text: String,
        query: String,
        matchCase: Boolean = false,
        wholeWord: Boolean = false,
        useRegex: Boolean = false
    ): List<TextRange> {
        if (query.isEmpty()) return emptyList()
        
        try {
            val pattern = buildPattern(query, matchCase, wholeWord, useRegex)
            val regex = pattern.toRegex()
            
            return regex.findAll(text).map { match ->
                TextRange(match.range.first, match.range.last + 1)
            }.toList()
        } catch (e: Exception) {
            // Invalid regex pattern
            return emptyList()
        }
    }

    /**
     * Find next match from current position.
     *
     * @param text Source text
     * @param query Search query
     * @param fromOffset Start searching from this position
     * @param matchCase Case-sensitive
     * @param wholeWord Whole words only
     * @param useRegex Regex mode
     * @param wrap Wrap to beginning if no match after fromOffset
     * @return Next match range, or null if no match
     */
    fun findNext(
        text: String,
        query: String,
        fromOffset: Int,
        matchCase: Boolean = false,
        wholeWord: Boolean = false,
        useRegex: Boolean = false,
        wrap: Boolean = true
    ): TextRange? {
        val matches = findAllMatches(text, query, matchCase, wholeWord, useRegex)
        if (matches.isEmpty()) return null
        
        // Find first match after fromOffset
        val nextMatch = matches.firstOrNull { it.start >= fromOffset }
        
        // FR-4: Wrap to first match if no match after cursor
        return if (nextMatch != null) {
            nextMatch
        } else if (wrap) {
            matches.firstOrNull()
        } else {
            null
        }
    }

    /**
     * Find previous match from current position.
     *
     * @param text Source text
     * @param query Search query
     * @param fromOffset Start searching backwards from this position
     * @param matchCase Case-sensitive
     * @param wholeWord Whole words only
     * @param useRegex Regex mode
     * @param wrap Wrap to end if no match before fromOffset
     * @return Previous match range, or null if no match
     */
    fun findPrevious(
        text: String,
        query: String,
        fromOffset: Int,
        matchCase: Boolean = false,
        wholeWord: Boolean = false,
        useRegex: Boolean = false,
        wrap: Boolean = true
    ): TextRange? {
        val matches = findAllMatches(text, query, matchCase, wholeWord, useRegex)
        if (matches.isEmpty()) return null
        
        // Find last match before fromOffset
        val previousMatch = matches.lastOrNull { it.end <= fromOffset }
        
        // Wrap to last match if no match before cursor
        return if (previousMatch != null) {
            previousMatch
        } else if (wrap) {
            matches.lastOrNull()
        } else {
            null
        }
    }

    /**
     * Replace one match.
     *
     * @param text Source text
     * @param matchRange Range of text to replace
     * @param replaceText Replacement text
     * @return New text with replacement applied
     */
    fun replaceOne(
        text: String,
        matchRange: TextRange,
        replaceText: String
    ): String {
        return text.substring(0, matchRange.start) + 
               replaceText + 
               text.substring(matchRange.end)
    }

    /**
     * Replace all matches (FR-11: single operation for atomic undo).
     *
     * @param text Source text
     * @param query Search query
     * @param replaceText Replacement text
     * @param matchCase Case-sensitive
     * @param wholeWord Whole words only
     * @param useRegex Regex mode
     * @return New text with all replacements applied
     */
    fun replaceAll(
        text: String,
        query: String,
        replaceText: String,
        matchCase: Boolean = false,
        wholeWord: Boolean = false,
        useRegex: Boolean = false
    ): String {
        if (query.isEmpty()) return text
        
        try {
            val pattern = buildPattern(query, matchCase, wholeWord, useRegex)
            val regex = pattern.toRegex()
            
            return regex.replace(text, replaceText)
        } catch (e: Exception) {
            // Invalid regex
            return text
        }
    }

    /**
     * Build regex pattern from query and options.
     */
    private fun buildPattern(
        query: String,
        matchCase: Boolean,
        wholeWord: Boolean,
        useRegex: Boolean
    ): String {
        var pattern = if (useRegex) {
            query
        } else {
            Regex.escape(query)
        }
        
        // Whole word: wrap with word boundaries
        if (wholeWord) {
            pattern = "\\b$pattern\\b"
        }
        
        // Case insensitive: add regex flag
        return if (matchCase) {
            pattern
        } else {
            "(?i)$pattern"
        }
    }
}
