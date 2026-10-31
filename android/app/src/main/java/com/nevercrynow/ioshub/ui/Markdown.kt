package com.nevercrynow.ioshub.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 轻量 Markdown：README / Release 说明够用即可。
 * 只支持标题、段落、有序/无序列表、代码块、引用、分隔线；
 * 行内支持 **粗体**、`代码`、[链接](url)（链接只上色，不跳转）。
 * 表格与徽章图片直接跳过——手机窄屏上没有阅读价值。
 */
sealed interface MdBlock {
    data class Heading(val level: Int, val text: String) : MdBlock
    data class Paragraph(val text: String) : MdBlock
    data class Bullets(val items: List<String>) : MdBlock
    data class Ordered(val items: List<String>) : MdBlock
    data class Code(val text: String) : MdBlock
    data class Quote(val text: String) : MdBlock
    data object Divider : MdBlock
}

object Markdown {

    fun parse(raw: String, maxBlocks: Int = 140): List<MdBlock> {
        val normalized = raw.replace("\r\n", "\n").replace("\r", "\n")
        val blocks = mutableListOf<MdBlock>()
        val paragraph = mutableListOf<String>()
        val bullets = mutableListOf<String>()
        val ordered = mutableListOf<String>()
        val code = mutableListOf<String>()
        var inCode = false

        fun flushParagraph() {
            if (paragraph.isEmpty()) return
            val text = paragraph.joinToString(" ").trim()
            paragraph.clear()
            if (text.isNotEmpty()) blocks.add(MdBlock.Paragraph(text))
        }

        fun flushBullets() {
            if (bullets.isNotEmpty()) {
                blocks.add(MdBlock.Bullets(bullets.toList()))
                bullets.clear()
            }
        }

        fun flushOrdered() {
            if (ordered.isNotEmpty()) {
                blocks.add(MdBlock.Ordered(ordered.toList()))
                ordered.clear()
            }
        }

        fun flushLists() {
            flushBullets()
            flushOrdered()
        }

        fun flushAll() {
            flushParagraph()
            flushLists()
        }

        for (line in normalized.lines()) {
            if (blocks.size >= maxBlocks) break
            val trimmed = line.trim()

            if (trimmed.startsWith("```")) {
                if (inCode) {
                    blocks.add(MdBlock.Code(code.joinToString("\n")))
                    code.clear()
                    inCode = false
                } else {
                    flushAll()
                    inCode = true
                }
                continue
            }
            if (inCode) {
                code.add(line)
                continue
            }
            if (trimmed.isEmpty()) {
                flushAll()
                continue
            }
            if (trimmed.startsWith("<!--")) continue
            if (isTableSeparator(trimmed)) continue
            if (isImageOnly(trimmed)) continue
            if (isHtmlTag(trimmed)) continue

            val heading = headingLevel(trimmed)
            if (heading > 0) {
                flushAll()
                val text = stripInlineHtml(trimmed.substring(heading).trim())
                if (text.isNotEmpty()) blocks.add(MdBlock.Heading(heading, text))
                continue
            }
            if (trimmed == "---" || trimmed == "***" || trimmed == "___") {
                flushAll()
                blocks.add(MdBlock.Divider)
                continue
            }
            if (trimmed.startsWith(">")) {
                flushAll()
                val text = stripInlineHtml(trimmed.removePrefix(">").trim())
                if (text.isNotEmpty()) blocks.add(MdBlock.Quote(text))
                continue
            }

            val bullet = listItem(trimmed)
            if (bullet != null) {
                flushParagraph()
                flushOrdered()
                bullets.add(stripInlineHtml(bullet))
                continue
            }
            val orderedItem = orderedItem(trimmed)
            if (orderedItem != null) {
                flushParagraph()
                flushBullets()
                ordered.add(stripInlineHtml(orderedItem))
                continue
            }

            paragraph.add(stripInlineHtml(trimmed))
        }

        if (inCode && code.isNotEmpty()) {
            blocks.add(MdBlock.Code(code.joinToString("\n")))
        }
        flushAll()
        return blocks.take(maxBlocks)
    }

    /** 行内样式：粗体、代码、链接。 */
    fun inline(text: String): AnnotatedString = buildAnnotatedString {
        var index = 0
        while (index < text.length) {
            when {
                text.startsWith("**", index) -> {
                    val end = text.indexOf("**", index + 2)
                    if (end > index + 2) {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(text.substring(index + 2, end))
                        }
                        index = end + 2
                    } else {
                        append("**")
                        index += 2
                    }
                }

                text[index] == '`' -> {
                    val end = text.indexOf('`', index + 1)
                    if (end > index) {
                        withStyle(
                            SpanStyle(
                                fontFamily = FontFamily.Monospace,
                                background = Color(0x14000000),
                            ),
                        ) {
                            append(text.substring(index + 1, end))
                        }
                        index = end + 1
                    } else {
                        append('`')
                        index += 1
                    }
                }

                text.startsWith("[", index) -> {
                    val closeBracket = text.indexOf(']', index)
                    val openParen = if (closeBracket > 0) text.indexOf('(', closeBracket) else -1
                    val closeParen = if (openParen > 0) text.indexOf(')', openParen) else -1
                    if (closeBracket > 0 && openParen == closeBracket + 1 && closeParen > openParen) {
                        withStyle(
                            SpanStyle(
                                color = Brand,
                                textDecoration = TextDecoration.Underline,
                            ),
                        ) {
                            append(text.substring(index + 1, closeBracket))
                        }
                        index = closeParen + 1
                    } else {
                        append(text[index])
                        index += 1
                    }
                }

                else -> {
                    append(text[index])
                    index += 1
                }
            }
        }
    }

    // MARK: 行判定

    private fun headingLevel(line: String): Int {
        val hashes = line.takeWhile { it == '#' }.length
        if (hashes == 0 || hashes > 6) return 0
        if (line.length <= hashes || line[hashes] != ' ') return 0
        return hashes
    }

    private fun listItem(line: String): String? {
        for (marker in listOf("- ", "* ", "+ ")) {
            if (line.startsWith(marker)) return line.removePrefix(marker).trim()
        }
        return null
    }

    private fun orderedItem(line: String): String? {
        val dot = line.indexOf('.')
        if (dot <= 0 || dot > 3) return null
        if (!line.substring(0, dot).all { it.isDigit() }) return null
        if (dot + 1 >= line.length || line[dot + 1] != ' ') return null
        return line.substring(dot + 2).trim()
    }

    private fun isTableSeparator(line: String): Boolean {
        if (!line.contains('-') || !line.contains('|')) return false
        return line.all { it == '|' || it == '-' || it == ':' || it == ' ' }
    }

    private fun isImageOnly(line: String): Boolean =
        line.startsWith("![") && line.contains("](") && line.endsWith(")")

    private fun isHtmlTag(line: String): Boolean =
        line.startsWith("<") && line.endsWith(">") && !line.contains(" ") && line.length < 60

    private fun stripInlineHtml(text: String): String {
        var value = Regex("<[^>]+>").replace(text, "")
        value = value.replace("&nbsp;", " ").replace("&amp;", "&")
            .replace("&lt;", "<").replace("&gt;", ">")
        return value.trim()
    }
}

// MARK: - 渲染

@Composable
fun MarkdownBody(blocks: List<MdBlock>, compact: Boolean = true) {
    val bodySize = if (compact) 12.sp else 13.sp
    val lineHeight = if (compact) 18.sp else 20.sp

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        blocks.forEach { block ->
            when (block) {
                is MdBlock.Heading -> Text(
                    text = Markdown.inline(block.text),
                    fontSize = when (block.level) {
                        1 -> 16.sp
                        2 -> 14.sp
                        else -> 13.sp
                    },
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = if (block.level <= 2) 4.dp else 0.dp),
                )

                is MdBlock.Paragraph -> Text(
                    text = Markdown.inline(block.text),
                    fontSize = bodySize,
                    lineHeight = lineHeight,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                is MdBlock.Bullets -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    block.items.forEach { item -> MarkdownRow(marker = "•", text = item, bodySize = bodySize, lineHeight = lineHeight) }
                }

                is MdBlock.Ordered -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    block.items.forEachIndexed { index, item ->
                        MarkdownRow(marker = "${index + 1}.", text = item, bodySize = bodySize, lineHeight = lineHeight)
                    }
                }

                is MdBlock.Code -> Box(
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
                        .padding(10.dp),
                ) {
                    Text(
                        text = block.text,
                        fontSize = 10.5f.sp,
                        lineHeight = 15.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                is MdBlock.Quote -> Text(
                    text = Markdown.inline("▎ ${block.text}"),
                    fontSize = bodySize,
                    lineHeight = lineHeight,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                MdBlock.Divider -> HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            }
        }
    }
}

@Composable
private fun MarkdownRow(marker: String, text: String, bodySize: androidx.compose.ui.unit.TextUnit, lineHeight: androidx.compose.ui.unit.TextUnit) {
    Row {
        Text(
            text = marker,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = Brand.copy(alpha = 0.8f),
            modifier = Modifier.width(16.dp),
        )
        Text(
            text = Markdown.inline(text),
            fontSize = bodySize,
            lineHeight = lineHeight,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
    }
}
