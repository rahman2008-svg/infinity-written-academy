package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MarkdownRenderer(
    markdown: String,
    modifier: Modifier = Modifier,
    baseFontSizeSp: Float = 16f,
    lineHeightFactor: Float = 1.45f,
    searchQuery: String = ""
) {
    val lines = markdown.lines()
    var inCodeBlock = false
    val codeBlockBuilder = StringBuilder()

    Column(modifier = modifier.fillMaxWidth()) {
        for (line in lines) {
            val trimmed = line.trim()

            // Code block handling
            if (trimmed.startsWith("```")) {
                if (inCodeBlock) {
                    // Close block
                    CodeBlockCard(
                        code = codeBlockBuilder.toString().trimEnd(),
                        fontSizeSp = baseFontSizeSp - 2f
                    )
                    codeBlockBuilder.clear()
                    inCodeBlock = false
                } else {
                    inCodeBlock = true
                }
                continue
            }

            if (inCodeBlock) {
                codeBlockBuilder.appendLine(line)
                continue
            }

            // Divider
            if (trimmed == "---" || trimmed == "***" || trimmed == "___") {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                )
                Spacer(modifier = Modifier.height(12.dp))
                continue
            }

            // Headings
            when {
                trimmed.startsWith("### ") -> {
                    Spacer(modifier = Modifier.height(10.dp))
                    HeadingText(
                        text = trimmed.removePrefix("### ").trim(),
                        fontSizeSp = baseFontSizeSp + 2f,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.SemiBold,
                        searchQuery = searchQuery
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
                trimmed.startsWith("## ") -> {
                    Spacer(modifier = Modifier.height(16.dp))
                    HeadingText(
                        text = trimmed.removePrefix("## ").trim(),
                        fontSizeSp = baseFontSizeSp + 5f,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        searchQuery = searchQuery
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                trimmed.startsWith("# ") -> {
                    Spacer(modifier = Modifier.height(20.dp))
                    HeadingText(
                        text = trimmed.removePrefix("# ").trim(),
                        fontSizeSp = baseFontSizeSp + 8f,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.ExtraBold,
                        searchQuery = searchQuery
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
                trimmed.startsWith("> ") -> {
                    // Blockquote
                    Spacer(modifier = Modifier.height(6.dp))
                    BlockquoteCard(
                        text = trimmed.removePrefix("> ").trim(),
                        fontSizeSp = baseFontSizeSp,
                        lineHeightFactor = lineHeightFactor,
                        searchQuery = searchQuery
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
                trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                    // Bullet list item
                    val bulletText = trimmed.substring(2).trim()
                    BulletListItem(
                        text = bulletText,
                        fontSizeSp = baseFontSizeSp,
                        lineHeightFactor = lineHeightFactor,
                        searchQuery = searchQuery
                    )
                }
                trimmed.matches(Regex("^\\d+\\.\\s.*")) -> {
                    // Numbered list item
                    val prefix = trimmed.substringBefore(".") + "."
                    val content = trimmed.substringAfter(".").trim()
                    NumberedListItem(
                        number = prefix,
                        text = content,
                        fontSizeSp = baseFontSizeSp,
                        lineHeightFactor = lineHeightFactor,
                        searchQuery = searchQuery
                    )
                }
                trimmed.isEmpty() -> {
                    Spacer(modifier = Modifier.height(8.dp))
                }
                else -> {
                    // Regular paragraph text
                    val annotated = parseInlineFormatting(
                        text = trimmed,
                        searchQuery = searchQuery,
                        primaryColor = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = annotated,
                        fontSize = baseFontSizeSp.sp,
                        lineHeight = (baseFontSizeSp * lineHeightFactor).sp,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(vertical = 3.dp)
                    )
                }
            }
        }

        // Catch unclosed code block if any
        if (inCodeBlock && codeBlockBuilder.isNotEmpty()) {
            CodeBlockCard(
                code = codeBlockBuilder.toString().trimEnd(),
                fontSizeSp = baseFontSizeSp - 2f
            )
        }
    }
}

@Composable
private fun HeadingText(
    text: String,
    fontSizeSp: Float,
    color: Color,
    fontWeight: FontWeight,
    searchQuery: String
) {
    val annotated = parseInlineFormatting(text, searchQuery, color)
    Text(
        text = annotated,
        fontSize = fontSizeSp.sp,
        fontWeight = fontWeight,
        color = color,
        lineHeight = (fontSizeSp * 1.3f).sp
    )
}

@Composable
private fun BulletListItem(
    text: String,
    fontSizeSp: Float,
    lineHeightFactor: Float,
    searchQuery: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, top = 3.dp, bottom = 3.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = (fontSizeSp * 0.45f).dp)
                .width(6.dp)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(MaterialTheme.colorScheme.primary)
        )
        Spacer(modifier = Modifier.width(10.dp))
        val annotated = parseInlineFormatting(text, searchQuery, MaterialTheme.colorScheme.onBackground)
        Text(
            text = annotated,
            fontSize = fontSizeSp.sp,
            lineHeight = (fontSizeSp * lineHeightFactor).sp,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun NumberedListItem(
    number: String,
    text: String,
    fontSizeSp: Float,
    lineHeightFactor: Float,
    searchQuery: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 6.dp, top = 3.dp, bottom = 3.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = number,
            fontWeight = FontWeight.Bold,
            fontSize = fontSizeSp.sp,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(28.dp)
        )
        val annotated = parseInlineFormatting(text, searchQuery, MaterialTheme.colorScheme.onBackground)
        Text(
            text = annotated,
            fontSize = fontSizeSp.sp,
            lineHeight = (fontSizeSp * lineHeightFactor).sp,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun BlockquoteCard(
    text: String,
    fontSizeSp: Float,
    lineHeightFactor: Float,
    searchQuery: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(36.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.primary)
        )
        Spacer(modifier = Modifier.width(10.dp))
        val annotated = parseInlineFormatting(text, searchQuery, MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = annotated,
            fontSize = fontSizeSp.sp,
            fontStyle = FontStyle.Italic,
            lineHeight = (fontSizeSp * lineHeightFactor).sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CodeBlockCard(code: String, fontSizeSp: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp)
    ) {
        Text(
            text = code,
            fontFamily = FontFamily.Monospace,
            fontSize = fontSizeSp.sp,
            lineHeight = (fontSizeSp * 1.35f).sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Parses bold (**text**), italic (*text*), code (`text`), and applies search highlight.
 */
fun parseInlineFormatting(
    text: String,
    searchQuery: String,
    primaryColor: Color
): AnnotatedString {
    return buildAnnotatedString {
        var cursor = 0
        val len = text.length

        while (cursor < len) {
            when {
                // Bold (**text**)
                text.startsWith("**", cursor) -> {
                    val end = text.indexOf("**", cursor + 2)
                    if (end != -1) {
                        val boldContent = text.substring(cursor + 2, end)
                        appendAnnotatedFragment(boldContent, searchQuery, isBold = true)
                        cursor = end + 2
                    } else {
                        append(text[cursor])
                        cursor++
                    }
                }
                // Inline code (`text`)
                text.startsWith("`", cursor) -> {
                    val end = text.indexOf("`", cursor + 1)
                    if (end != -1) {
                        val codeContent = text.substring(cursor + 1, end)
                        pushStyle(
                            SpanStyle(
                                fontFamily = FontFamily.Monospace,
                                background = Color(0x22888888)
                            )
                        )
                        append(codeContent)
                        pop()
                        cursor = end + 1
                    } else {
                        append(text[cursor])
                        cursor++
                    }
                }
                // Italic (*text*)
                text.startsWith("*", cursor) -> {
                    val end = text.indexOf("*", cursor + 1)
                    if (end != -1) {
                        val italicContent = text.substring(cursor + 1, end)
                        appendAnnotatedFragment(italicContent, searchQuery, isItalic = true)
                        cursor = end + 1
                    } else {
                        append(text[cursor])
                        cursor++
                    }
                }
                else -> {
                    // Regular character or search match
                    if (searchQuery.isNotEmpty() && cursor + searchQuery.length <= len &&
                        text.substring(cursor, cursor + searchQuery.length).equals(searchQuery, ignoreCase = true)
                    ) {
                        pushStyle(
                            SpanStyle(
                                background = Color(0xFFFFE082),
                                color = Color(0xFF1E1E1E),
                                fontWeight = FontWeight.Bold
                            )
                        )
                        append(text.substring(cursor, cursor + searchQuery.length))
                        pop()
                        cursor += searchQuery.length
                    } else {
                        append(text[cursor])
                        cursor++
                    }
                }
            }
        }
    }
}

private fun AnnotatedString.Builder.appendAnnotatedFragment(
    fragment: String,
    searchQuery: String,
    isBold: Boolean = false,
    isItalic: Boolean = false
) {
    val style = SpanStyle(
        fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
        fontStyle = if (isItalic) FontStyle.Italic else FontStyle.Normal
    )
    pushStyle(style)

    if (searchQuery.isNotEmpty() && fragment.contains(searchQuery, ignoreCase = true)) {
        var start = 0
        val lowerFrag = fragment.lowercase()
        val lowerQ = searchQuery.lowercase()
        while (start < fragment.length) {
            val idx = lowerFrag.indexOf(lowerQ, start)
            if (idx != -1) {
                if (idx > start) {
                    append(fragment.substring(start, idx))
                }
                pushStyle(
                    SpanStyle(
                        background = Color(0xFFFFE082),
                        color = Color(0xFF1E1E1E),
                        fontWeight = FontWeight.Bold
                    )
                )
                append(fragment.substring(idx, idx + searchQuery.length))
                pop()
                start = idx + searchQuery.length
            } else {
                append(fragment.substring(start))
                break
            }
        }
    } else {
        append(fragment)
    }

    pop()
}
