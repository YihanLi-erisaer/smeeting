package com.stardazz.smeeting.feature.history

/**
 * Qwen / similar models may emit `<redacted_thinking>…</redacted_thinking>` around internal reasoning.
 * [copyPlainText] strips that block from clipboard when the closing tag is present.
 */
private data class ParsedAiSummary(
    val thinking: String?,
    val visible: String,
    val hasUnclosedThinking: Boolean,
)

object AiSummaryThinkingParser {

    private const val OPEN = "<redacted_thinking>"
    private const val CLOSE = "</redacted_thinking>"

    private fun parse(raw: String): ParsedAiSummary {
        val lower = raw.lowercase()
        val idxOpen = lower.indexOf(OPEN.lowercase())
        if (idxOpen < 0) {
            return ParsedAiSummary(null, raw.trim(), false)
        }
        val startContent = idxOpen + OPEN.length
        val idxClose = lower.indexOf(CLOSE.lowercase(), startIndex = startContent)
        val prefixBefore = raw.substring(0, idxOpen).trimEnd()
        if (idxClose < 0) {
            val thinking = raw.substring(startContent)
            return ParsedAiSummary(
                thinking = thinking,
                visible = prefixBefore,
                hasUnclosedThinking = true,
            )
        }
        val thinking = raw.substring(startContent, idxClose).trim()
        val suffix = raw.substring(idxClose + CLOSE.length).trimStart()
        val visible = listOf(prefixBefore, suffix).filter { it.isNotBlank() }.joinToString("\n\n").trim()
        return ParsedAiSummary(
            thinking = thinking.ifBlank { null },
            visible = visible,
            hasUnclosedThinking = false,
        )
    }

    /**
     * Clipboard: after a closed thinking block, copy only the visible answer.
     * While thinking is still open or there are no tags, copy the full trimmed text.
     */
    fun copyPlainText(raw: String): String {
        val p = parse(raw.trim())
        return when {
            p.thinking == null -> raw.trim()
            p.hasUnclosedThinking -> raw.trim()
            else -> p.visible.ifBlank { raw.trim() }
        }
    }
}
