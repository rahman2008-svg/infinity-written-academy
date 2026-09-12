package com.example.data.model

data class WritingItem(
    val id: String,
    val title: String,
    val category: String,
    val level: String = "মাধ্যমিক / Secondary",
    val language: String = "Bengali", // "Bengali" or "English"
    val tags: List<String> = emptyList(),
    val summary: String = "",
    val assetPath: String = ""
)

data class ContentDetail(
    val item: WritingItem,
    val rawMarkdown: String,
    val questionTopic: String = "",
    val formatStructure: String = "",
    val modelAnswer: String = "",
    val keyPoints: String = "",
    val writingTips: String = ""
)

data class SearchResult(
    val item: WritingItem,
    val matchSnippet: String,
    val matchField: String
)

data class CategoryMeta(
    val id: String,
    val titleBangla: String,
    val titleEnglish: String,
    val iconEmoji: String,
    val language: String,
    val description: String,
    val count: Int = 0
)

data class QuickRevisionItem(
    val id: String,
    val title: String,
    val category: String,
    val language: String,
    val iconEmoji: String,
    val keyStructure: List<String>,
    val tips: List<String>,
    val sampleItemId: String? = null
)
