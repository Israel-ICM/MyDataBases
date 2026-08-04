package com.sphynxs.mydatabases.domain.sql

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.BufferedReader
import java.io.Reader

/**
 * Streaming, comment/string/backtick/`DELIMITER`-aware SQL statement splitter.
 *
 * See `openspec/changes/large-sql-script-execution/specs/sql-statement-stream-splitting/spec.md`
 * for the full requirement set. This is the correctness-critical component (R4) of large-script
 * execution: a mis-split silently corrupts what runs against the database, so ambiguity MUST
 * fail loud via [ScriptError] — it never guesses.
 *
 * Design notes:
 * - Consumes a [Reader] character-by-character; never materializes the full input as a `String`.
 *   Peak retained memory is bounded by the largest single statement (or, in the pathological
 *   case of scanning one unusually long physical line for a `DELIMITER` directive, by that
 *   line's length — still bounded by "the largest thing currently being parsed", never by total
 *   file size).
 * - `DELIMITER` directive detection uses [BufferedReader.mark]/[BufferedReader.reset]: at the
 *   start of every physical line where no statement content has accumulated yet, it tentatively
 *   reads the whole line, decides whether it is a directive, and either consumes it (directive)
 *   or rewinds and replays it through the exact same character-by-character state machine used
 *   for live input (not a directive) — this avoids a second, divergent "replay" code path.
 *
 * @author sdd-apply (Strict TDD)
 * @date 2026-08-04
 */
object SqlStatementStreamSplitter {

    private const val DEFAULT_TERMINATOR = ";"

    /** Generous mark limit for the one-physical-line DELIMITER sniff; grows lazily, not preallocated. */
    private const val LINE_SNIFF_MARK_LIMIT = 50_000_000

    private val DELIMITER_PREFIX_REGEX = Regex("(?i)^delimiter\\b")
    private val DELIMITER_DIRECTIVE_REGEX = Regex("(?i)^delimiter\\s+(\\S+)\\s*$")

    private enum class LexState { SINGLE_QUOTE, DOUBLE_QUOTE, BACKTICK, LINE_COMMENT, BLOCK_COMMENT }

    fun split(reader: Reader): Flow<ScriptStatement> = flow {
        val br: BufferedReader = if (reader is BufferedReader) reader else BufferedReader(reader)

        var lineNumber = 1
        var statementStartLine = 1
        var blockCommentStartLine = 1
        var activeTerminator = DEFAULT_TERMINATOR
        var parenDepth = 0
        var hasTopLevelWhere = false
        var atLineStart = true
        var quoteOrCommentState: LexState? = null
        val buffer = StringBuilder()
        val currentWord = StringBuilder()

        fun peekChar(): Int {
            br.mark(1)
            val c = br.read()
            br.reset()
            return c
        }

        fun flushWord() {
            if (currentWord.isNotEmpty()) {
                if (parenDepth == 0 && currentWord.toString().equals("WHERE", ignoreCase = true)) {
                    hasTopLevelWhere = true
                }
                currentWord.clear()
            }
        }

        fun markStartIfNeeded() {
            if (buffer.isBlank()) statementStartLine = lineNumber
        }

        suspend fun emitIfTerminated() {
            if (parenDepth == 0 &&
                activeTerminator.isNotEmpty() &&
                buffer.length >= activeTerminator.length &&
                buffer.endsWith(activeTerminator)
            ) {
                flushWord()
                val content = buffer.substring(0, buffer.length - activeTerminator.length).trim()
                if (content.isNotEmpty()) {
                    emit(ScriptStatement(content, statementStartLine, hasTopLevelWhere))
                }
                buffer.clear()
                hasTopLevelWhere = false
            }
        }

        /** Handles one character while inside a quoted/backtick region. Returns true if the region closed. */
        fun handleQuotedChar(c: Char, quoteChar: Char): Boolean {
            buffer.append(c)
            return when (c) {
                '\\' -> {
                    val next = br.read()
                    if (next != -1) {
                        buffer.append(next.toChar())
                        if (next.toChar() == '\n') lineNumber++
                    }
                    false
                }
                '\n' -> {
                    lineNumber++
                    false
                }
                quoteChar -> {
                    val p = peekChar()
                    if (p != -1 && p.toChar() == quoteChar) {
                        br.read() // consume the doubled quote char — literal, region stays open
                        buffer.append(quoteChar)
                        false
                    } else {
                        true // genuine close
                    }
                }
                else -> false
            }
        }

        suspend fun processChar(c: Char) {
            val state = quoteOrCommentState
            if (state == null) {
                when (c) {
                    '\'' -> {
                        markStartIfNeeded(); flushWord(); buffer.append(c)
                        quoteOrCommentState = LexState.SINGLE_QUOTE; atLineStart = false
                    }
                    '"' -> {
                        markStartIfNeeded(); flushWord(); buffer.append(c)
                        quoteOrCommentState = LexState.DOUBLE_QUOTE; atLineStart = false
                    }
                    '`' -> {
                        markStartIfNeeded(); flushWord(); buffer.append(c)
                        quoteOrCommentState = LexState.BACKTICK; atLineStart = false
                    }
                    '(' -> {
                        markStartIfNeeded(); flushWord(); buffer.append(c)
                        parenDepth++; atLineStart = false
                    }
                    ')' -> {
                        markStartIfNeeded(); flushWord(); buffer.append(c)
                        if (parenDepth > 0) parenDepth--
                        atLineStart = false
                    }
                    '-' -> {
                        val p = peekChar()
                        if (p != -1 && p.toChar() == '-') {
                            br.read()
                            flushWord()
                            quoteOrCommentState = LexState.LINE_COMMENT
                            atLineStart = false
                        } else {
                            markStartIfNeeded(); buffer.append(c); currentWord.clear(); atLineStart = false
                        }
                    }
                    '#' -> {
                        flushWord()
                        quoteOrCommentState = LexState.LINE_COMMENT
                        atLineStart = false
                    }
                    '/' -> {
                        val p = peekChar()
                        if (p != -1 && p.toChar() == '*') {
                            br.read()
                            flushWord()
                            blockCommentStartLine = lineNumber
                            quoteOrCommentState = LexState.BLOCK_COMMENT
                            atLineStart = false
                        } else {
                            markStartIfNeeded(); buffer.append(c); currentWord.clear(); atLineStart = false
                        }
                    }
                    '\n' -> {
                        markStartIfNeeded()
                        buffer.append(c)
                        flushWord()
                        lineNumber++
                        atLineStart = true
                    }
                    else -> {
                        markStartIfNeeded()
                        buffer.append(c)
                        if (c.isLetterOrDigit() || c == '_') {
                            currentWord.append(c)
                        } else {
                            flushWord()
                        }
                        if (!c.isWhitespace()) atLineStart = false
                    }
                }
                emitIfTerminated()
            } else {
                when (state) {
                    LexState.SINGLE_QUOTE -> if (handleQuotedChar(c, '\'')) quoteOrCommentState = null
                    LexState.DOUBLE_QUOTE -> if (handleQuotedChar(c, '"')) quoteOrCommentState = null
                    LexState.BACKTICK -> if (handleQuotedChar(c, '`')) quoteOrCommentState = null
                    LexState.LINE_COMMENT -> {
                        if (c == '\n') {
                            lineNumber++
                            atLineStart = true
                            quoteOrCommentState = null
                        }
                    }
                    LexState.BLOCK_COMMENT -> {
                        if (c == '\n') lineNumber++
                        if (c == '*') {
                            val p = peekChar()
                            if (p != -1 && p.toChar() == '/') {
                                br.read()
                                quoteOrCommentState = null
                            }
                        }
                    }
                }
            }
        }

        var skipSniffOnce = false
        while (true) {
            if (!skipSniffOnce && quoteOrCommentState == null && atLineStart && buffer.isBlank()) {
                br.mark(LINE_SNIFF_MARK_LIMIT)
                val lineBuilder = StringBuilder()
                var ch = br.read()
                while (ch != -1 && ch.toChar() != '\n') {
                    lineBuilder.append(ch.toChar())
                    ch = br.read()
                }
                val hasNewline = ch != -1
                val trimmedLine = lineBuilder.toString().trim()

                if (trimmedLine.isNotEmpty() && DELIMITER_PREFIX_REGEX.containsMatchIn(trimmedLine)) {
                    val match = DELIMITER_DIRECTIVE_REGEX.find(trimmedLine)
                    if (match != null) {
                        activeTerminator = match.groupValues[1]
                        if (hasNewline) lineNumber++
                        atLineStart = true
                        continue
                    } else {
                        throw ScriptError.MalformedDelimiterDirective(lineNumber)
                    }
                } else {
                    br.reset()
                    skipSniffOnce = true
                    continue
                }
            }
            skipSniffOnce = false

            val next = br.read()
            if (next == -1) break
            processChar(next.toChar())
        }

        if (quoteOrCommentState == LexState.BLOCK_COMMENT) {
            throw ScriptError.UnterminatedToken(blockCommentStartLine, "block comment")
        }

        val finalContent = buffer.toString().trim()
        if (finalContent.isNotEmpty()) {
            emit(ScriptStatement(finalContent, statementStartLine, hasTopLevelWhere))
        }
    }
}
