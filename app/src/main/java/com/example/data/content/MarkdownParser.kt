package com.example.data.content

import com.example.data.model.ContentDetail
import com.example.data.model.WritingItem

object MarkdownParser {

    /**
     * Extracts front-matter only without loading or parsing the rest of the file into memory.
     */
    fun parseFrontMatterOnly(content: String, assetPath: String): WritingItem? {
        if (!content.startsWith("---")) return null
        val endIndex = content.indexOf("\n---", 3)
        if (endIndex == -1) return null

        val frontMatter = content.substring(3, endIndex).trim()
        val lines = frontMatter.lines()

        var id = ""
        var title = ""
        var category = ""
        var level = "মাধ্যমিক / Secondary"
        var language = if (assetPath.contains("/bangla/")) "Bengali" else "English"
        var summary = ""
        val tags = mutableListOf<String>()

        var inTags = false
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue

            if (inTags) {
                if (trimmed.startsWith("-")) {
                    tags.add(trimmed.substring(1).trim())
                    continue
                } else {
                    inTags = false
                }
            }

            val colonIndex = trimmed.indexOf(':')
            if (colonIndex != -1) {
                val key = trimmed.substring(0, colonIndex).trim().lowercase()
                val value = trimmed.substring(colonIndex + 1).trim().removeSurrounding("\"").removeSurrounding("'")
                when (key) {
                    "id" -> id = value
                    "title" -> title = value
                    "category" -> category = value
                    "level" -> if (value.isNotEmpty()) level = value
                    "language" -> if (value.isNotEmpty()) language = value
                    "summary" -> summary = value
                    "tags" -> {
                        inTags = true
                        if (value.isNotEmpty() && value != "[]") {
                            value.split(",").map { it.trim().removeSurrounding("\"").removeSurrounding("'") }
                                .filter { it.isNotEmpty() }
                                .forEach { tags.add(it) }
                        }
                    }
                }
            }
        }

        if (id.isEmpty()) {
            id = assetPath.substringAfterLast("/").substringBeforeLast(".md")
        }
        if (title.isEmpty()) {
            title = id.replace("-", " ").replace("_", " ").capitalizeWords()
        }
        if (category.isEmpty()) {
            category = deriveCategoryFromPath(assetPath, language)
        }

        return WritingItem(
            id = id,
            title = title,
            category = category,
            level = level,
            language = language,
            tags = tags,
            summary = summary,
            assetPath = assetPath
        )
    }

    /**
     * Parses full markdown content into structured sections for Normal View, Exam View, and Practice Mode.
     */
    fun parseFullContent(rawContent: String, item: WritingItem): ContentDetail {
        val bodyContent: String
        if (rawContent.startsWith("---")) {
            val endIndex = rawContent.indexOf("\n---", 3)
            bodyContent = if (endIndex != -1) {
                rawContent.substring(endIndex + 4).trim()
            } else {
                rawContent
            }
        } else {
            bodyContent = rawContent.trim()
        }

        // Split into sections by Markdown headings
        val sections = extractSections(bodyContent)

        val question = sections.entries.firstOrNull {
            it.key.contains("প্রশ্ন", ignoreCase = true) ||
            it.key.contains("topic", ignoreCase = true) ||
            it.key.contains("question", ignoreCase = true)
        }?.value ?: ""

        val format = sections.entries.firstOrNull {
            it.key.contains("কাঠামো", ignoreCase = true) ||
            it.key.contains("format", ignoreCase = true) ||
            it.key.contains("নিয়ম", ignoreCase = true) ||
            it.key.contains("structure", ignoreCase = true)
        }?.value ?: ""

        val modelAnswer = sections.entries.firstOrNull {
            it.key.contains("মূল উত্তর", ignoreCase = true) ||
            it.key.contains("model answer", ignoreCase = true) ||
            it.key.contains("সারাংশ", ignoreCase = true) ||
            it.key.contains("সারমর্ম", ignoreCase = true) ||
            it.key.contains("সম্প্রসারিত ভাব", ignoreCase = true) ||
            it.key.contains("মূল বক্তব্য", ignoreCase = true) ||
            it.key.contains("letter", ignoreCase = true) ||
            it.key.contains("application", ignoreCase = true) ||
            it.key.contains("paragraph", ignoreCase = true) ||
            it.key.contains("composition", ignoreCase = true) ||
            it.key.contains("রচনা", ignoreCase = true)
        }?.value ?: bodyContent

        val keyPoints = sections.entries.firstOrNull {
            it.key.contains("পয়েন্ট", ignoreCase = true) ||
            it.key.contains("শব্দ", ignoreCase = true) ||
            it.key.contains("key points", ignoreCase = true) ||
            it.key.contains("vocabulary", ignoreCase = true)
        }?.value ?: ""

        val tips = sections.entries.firstOrNull {
            it.key.contains("টিপস", ignoreCase = true) ||
            it.key.contains("tips", ignoreCase = true) ||
            it.key.contains("কৌশল", ignoreCase = true)
        }?.value ?: ""

        return ContentDetail(
            item = item,
            rawMarkdown = bodyContent,
            questionTopic = question,
            formatStructure = format,
            modelAnswer = modelAnswer,
            keyPoints = keyPoints,
            writingTips = tips
        )
    }

    private fun extractSections(markdown: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        val lines = markdown.lines()
        var currentHeading = "Intro"
        val currentContent = StringBuilder()

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("## ") || trimmed.startsWith("# ")) {
                if (currentContent.isNotEmpty()) {
                    result[currentHeading] = currentContent.toString().trim()
                    currentContent.clear()
                }
                currentHeading = trimmed.removePrefix("## ").removePrefix("# ").trim()
            } else {
                currentContent.appendLine(line)
            }
        }
        if (currentContent.isNotEmpty()) {
            result[currentHeading] = currentContent.toString().trim()
        }
        return result
    }

    private fun deriveCategoryFromPath(path: String, language: String): String {
        return when {
            path.contains("/summary/") -> if (language == "Bengali") "সারাংশ" else "Summary"
            path.contains("/saramarma/") -> "সারমর্ম"
            path.contains("/bhab-somprosaron/") -> "ভাবসম্প্রসারণ"
            path.contains("/letters/") -> if (language == "Bengali") "চিঠিপত্র" else "Letter"
            path.contains("/applications/") -> if (language == "Bengali") "দরখাস্ত" else "Application"
            path.contains("/emails/") -> "Email"
            path.contains("/compositions/") -> "Composition"
            path.contains("/essays/") -> if (language == "Bengali") "রচনা" else "Essay"
            path.contains("/paragraphs/") -> if (language == "Bengali") "অনুচ্ছেদ" else "Paragraph"
            path.contains("/reports/") -> if (language == "Bengali") "প্রতিবেদন" else "Report"
            path.contains("/dialogues/") -> if (language == "Bengali") "সংলাপ" else "Dialogue"
            path.contains("/stories/") -> "Story Writing"
            path.contains("/writing-skills/") -> if (language == "Bengali") "লেখার কৌশল" else "Writing Skills"
            else -> if (language == "Bengali") "বাংলা লিখিত" else "English Written"
        }
    }

    private fun String.capitalizeWords(): String {
        return split(" ").joinToString(" ") { word ->
            word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
    }
}
