package com.example.data.content

import android.content.Context
import android.util.Log
import com.example.data.model.CategoryMeta
import com.example.data.model.ContentDetail
import com.example.data.model.QuickRevisionItem
import com.example.data.model.SearchResult
import com.example.data.model.WritingItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

class ContentRepository(private val context: Context) {

    private val TAG = "ContentRepository"
    private val _items = MutableStateFlow<List<WritingItem>>(emptyList())
    val items: StateFlow<List<WritingItem>> = _items.asStateFlow()

    private val _isIndexed = MutableStateFlow(false)
    val isIndexed: StateFlow<Boolean> = _isIndexed.asStateFlow()

    private val contentCache = mutableMapOf<String, ContentDetail>()

    suspend fun initializeIndex() = withContext(Dispatchers.IO) {
        if (_isIndexed.value && _items.value.isNotEmpty()) return@withContext

        val discoveredPaths = mutableListOf<String>()
        scanAssetDirectory("content", discoveredPaths)

        val parsedItems = mutableListOf<WritingItem>()
        val seenIds = mutableSetOf<String>()

        for (path in discoveredPaths) {
            try {
                // Read front-matter only (up to 1500 chars or second '---')
                val frontMatterRaw = readFrontMatterHeader(path)
                if (frontMatterRaw.isBlank()) {
                    Log.w(TAG, "Empty or unreadable markdown at $path, skipping")
                    continue
                }

                val item = MarkdownParser.parseFrontMatterOnly(frontMatterRaw, path)
                if (item == null) {
                    Log.w(TAG, "Failed to parse front-matter for $path, skipping")
                    continue
                }

                // Validation: handle duplicate ID gracefully
                var validId = item.id
                if (seenIds.contains(validId)) {
                    validId = "${validId}_${path.hashCode()}"
                }
                seenIds.add(validId)

                parsedItems.add(item.copy(id = validId))
            } catch (e: Exception) {
                Log.e(TAG, "Error indexing file $path: ${e.message}", e)
            }
        }

        _items.value = parsedItems
        _isIndexed.value = true
        Log.i(TAG, "Successfully indexed ${parsedItems.size} educational markdown documents offline.")
    }

    private fun scanAssetDirectory(path: String, outList: MutableList<String>) {
        try {
            val list = context.assets.list(path) ?: return
            for (file in list) {
                val fullPath = if (path.isEmpty()) file else "$path/$file"
                if (file.endsWith(".md", ignoreCase = true)) {
                    outList.add(fullPath)
                } else {
                    // Try recursing if directory
                    val sublist = context.assets.list(fullPath)
                    if (sublist != null && sublist.isNotEmpty()) {
                        scanAssetDirectory(fullPath, outList)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed listing assets at $path: ${e.message}")
        }
    }

    private fun readFrontMatterHeader(path: String): String {
        context.assets.open(path).use { input ->
            BufferedReader(InputStreamReader(input, Charsets.UTF_8)).use { reader ->
                val sb = StringBuilder()
                var dashesCount = 0
                var line: String? = reader.readLine()
                while (line != null) {
                    sb.appendLine(line)
                    if (line.trim() == "---") {
                        dashesCount++
                        if (dashesCount >= 2) break
                    }
                    if (sb.length > 2000) break
                    line = reader.readLine()
                }
                return sb.toString()
            }
        }
    }

    suspend fun loadFullContent(item: WritingItem): ContentDetail = withContext(Dispatchers.IO) {
        contentCache[item.id]?.let { return@withContext it }

        val rawContent = try {
            context.assets.open(item.assetPath).use { input ->
                input.bufferedReader(Charsets.UTF_8).readText()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed loading full content for ${item.assetPath}: ${e.message}")
            "# ${item.title}\n\nবিষয়বস্তু লোড করতে সমস্যা হয়েছে। অনুগ্রহ করে পুনরায় চেষ্টা করুন।"
        }

        val detail = MarkdownParser.parseFullContent(rawContent, item)
        contentCache[item.id] = detail
        detail
    }

    fun getItemById(id: String): WritingItem? {
        return _items.value.firstOrNull { it.id == id }
    }

    fun getItemsByCategory(category: String): List<WritingItem> {
        return _items.value.filter { it.category.equals(category, ignoreCase = true) }
    }

    fun getItemsByLanguage(language: String): List<WritingItem> {
        return _items.value.filter { it.language.equals(language, ignoreCase = true) }
    }

    suspend fun searchContent(
        query: String,
        languageFilter: String? = null
    ): List<SearchResult> = withContext(Dispatchers.Default) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return@withContext emptyList()

        val results = mutableListOf<SearchResult>()
        val pool = _items.value.filter {
            languageFilter == null || languageFilter == "All" || it.language.equals(languageFilter, ignoreCase = true)
        }

        for (item in pool) {
            val titleMatches = item.title.lowercase().contains(q)
            val categoryMatches = item.category.lowercase().contains(q)
            val tagsMatch = item.tags.any { it.lowercase().contains(q) }
            val summaryMatches = item.summary.lowercase().contains(q)

            if (titleMatches || categoryMatches || tagsMatch || summaryMatches) {
                val field = when {
                    titleMatches -> "Title"
                    categoryMatches -> "Category"
                    tagsMatch -> "Tag"
                    else -> "Summary"
                }
                val snippet = if (item.summary.isNotEmpty()) {
                    item.summary
                } else {
                    "${item.category} • ${item.level}"
                }
                results.add(SearchResult(item = item, matchSnippet = snippet, matchField = field))
            }
        }
        results
    }

    fun getAllCategories(): List<CategoryMeta> {
        val currentItems = _items.value
        val banglaCategories = listOf(
            CategoryMeta("summary", "সারাংশ", "Summary", "📖", "Bengali", "মূল বক্তব্য সংক্ষেপে উপস্থাপনের নিয়ম ও নমুনা উত্তর"),
            CategoryMeta("saramarma", "সারমর্ম", "Saramarma", "📖", "Bengali", "কবিতা বা গদ্যাংশের অন্তর্নিহিত অর্থ সুন্দরভাবে লেখা"),
            CategoryMeta("bhab-somprosaron", "ভাবসম্প্রসারণ", "Amplification", "💡", "Bengali", "উক্তি বা কাব্যাংশের ভাবকে যুক্তি ও উদাহরণ দিয়ে সম্প্রসারণ"),
            CategoryMeta("letters", "চিঠিপত্র", "Letters", "✉️", "Bengali", "ব্যক্তিগত ও সামাজিক বিভিন্ন ধরনের চিঠি লেখার আদর্শ নিয়ম"),
            CategoryMeta("applications", "দরখাস্ত", "Applications", "📝", "Bengali", "ছুটি, সাহায্য ও বিভিন্ন বিষয়ের প্রাতিষ্ঠানিক আবেদনপত্র"),
            CategoryMeta("essays", "রচনা", "Essays", "📚", "Bengali", "পয়েন্টভিত্তিক পূর্ণাঙ্গ ও সমৃদ্ধ মানসম্মত বাংলা প্রবন্ধ/রচনা"),
            CategoryMeta("paragraphs", "অনুচ্ছেদ", "Paragraphs", "📄", "Bengali", "পরীক্ষার উপযোগী সুনির্দিষ্ট ও স্পষ্ট আকারের বাংলা অনুচ্ছেদ"),
            CategoryMeta("reports", "প্রতিবেদন", "Reports", "📰", "Bengali", "সংবাদ প্রতিবেদন ও প্রাতিষ্ঠানিক প্রতিবেদন উপস্থাপনার কৌশল"),
            CategoryMeta("dialogues", "সংলাপ", "Dialogues", "💬", "Bengali", "সমসাময়িক বিষয়ে জীবন্ত ও প্রাসঙ্গিক কথোপকথন রচনা"),
            CategoryMeta("writing-skills", "লেখার কৌশল", "Writing Skills", "✍️", "Bengali", "পরীক্ষায় পূর্ণ নম্বর অর্জনের বিশেষ লেখার নিয়ম ও উপস্থাপনা")
        )

        val englishCategories = listOf(
            CategoryMeta("letters", "Letter", "Informal & Formal Letters", "✉️", "English", "Mastering academic formats, body organization and closing"),
            CategoryMeta("applications", "Application", "Formal Applications", "📝", "English", "Leave, concession, library, facilities official requests"),
            CategoryMeta("emails", "Email", "Academic & Casual Emails", "📧", "English", "Subject line writing, greetings, concise message and sign-offs"),
            CategoryMeta("compositions", "Composition", "Compositions", "📚", "English", "Detailed multi-paragraph essays with outlines and key points"),
            CategoryMeta("paragraphs", "Paragraph", "Paragraphs", "📄", "English", "Topic sentences, supporting points, and exam model answers"),
            CategoryMeta("reports", "Report", "Reports", "📰", "English", "School events, surveys, campaigns, and news reporting format"),
            CategoryMeta("dialogues", "Dialogue", "Dialogues", "💬", "English", "Real-world conversations with polite phrases and idioms"),
            CategoryMeta("stories", "Story Writing", "Story Writing", "📖", "English", "Plot building, conflict, resolution, and moral lessons"),
            CategoryMeta("essays", "Essay", "Essays", "📚", "English", "Analytical arguments, clear thesis and conclusions"),
            CategoryMeta("writing-skills", "Writing Skills", "Writing Skills", "✍️", "English", "Grammar, transition words, paragraph cohesion and exam tips")
        )

        val all = mutableListOf<CategoryMeta>()
        for (c in banglaCategories) {
            val count = currentItems.count { it.language == "Bengali" && it.category.contains(c.titleBangla, ignoreCase = true) }
            all.add(c.copy(count = count))
        }
        for (c in englishCategories) {
            val count = currentItems.count { it.language == "English" && it.category.contains(c.titleBangla, ignoreCase = true) }
            all.add(c.copy(count = count))
        }
        return all
    }

    fun getQuickRevisionItems(): List<QuickRevisionItem> {
        return listOf(
            QuickRevisionItem(
                id = "rev-bangla-app",
                title = "বাংলা দরখাস্ত কাঠামো",
                category = "দরখাস্ত",
                language = "Bengali",
                iconEmoji = "📝",
                keyStructure = listOf(
                    "১. তারিখ: আবেদন জমার সঠিক তারিখ",
                    "২. প্রাপক: বরাবর, প্রধান শিক্ষক / অধ্যক্ষ",
                    "৩. প্রতিষ্ঠানের নাম ও ঠিকানা (কাল্পনিক/স্থানধারক)",
                    "৪. বিষয়: আবেদনের সুনির্দিষ্ট উদ্দেশ্য (সংক্ষেপে)",
                    "৫. সম্বোধন: জনাব / মহোদয়,",
                    "৬. মূল বক্তব্য: বিনীত নিবেদন এই যে... (পরিস্থিতি ও প্রার্থিত সহায়তা)",
                    "৭. সমাপ্তি: অতএব, প্রার্থনা এই যে...",
                    "৮. বিনীত নিবেদক: নাম, শ্রেণি, রোল ও শাখা"
                ),
                tips = listOf(
                    "একটি পৃষ্ঠায় দরখাস্ত সম্পন্ন করার চেষ্টা করো।",
                    "বিষয় যেন এক লাইনের মধ্যে পরিষ্কার থাকে।",
                    "কোনো অপ্রয়োজনীয় বাহুল্য কথা এড়িয়ে চলো।"
                )
            ),
            QuickRevisionItem(
                id = "rev-bangla-letter",
                title = "বাংলা ব্যক্তিগত চিঠি কাঠামো",
                category = "চিঠিপত্র",
                language = "Bengali",
                iconEmoji = "✉️",
                keyStructure = listOf(
                    "১. মঙ্গলসূচক শব্দ (ঐচ্ছিক)",
                    "২. স্থান ও তারিখ: উপরে ডান কোণে",
                    "৩. সম্বোধন: প্রিয় [বন্ধুর নাম] / শ্রদ্ধেয় [আব্বা/আম্মা]",
                    "৪. কুশল বিনিময় ও ভূমিকা",
                    "৫. পত্রের মূল বিষয়বস্তু",
                    "৬. ইতি ও সমাপ্তি আশীর্বাদ/ভালোবাসা",
                    "৭. স্বাক্ষর / প্রেরকের নাম",
                    "৮. ডাকটিকিটসহ খামের নমুনা (প্রেরক ও প্রাপকের ঠিকানা)"
                ),
                tips = listOf(
                    "বন্ধুর ক্ষেত্রে আন্তরিক ও গুরুজনদের ক্ষেত্রে শ্রদ্ধাপূর্ণ ভাষা ব্যবহার করো।",
                    "শেষে খাম আঁকা আবশ্যক।"
                )
            ),
            QuickRevisionItem(
                id = "rev-bangla-summary",
                title = "সারাংশ ও সারমর্ম নিয়ম",
                category = "সারাংশ / সারমর্ম",
                language = "Bengali",
                iconEmoji = "📖",
                keyStructure = listOf(
                    "১. অনুচ্ছেদ বা কবিতাটি ২-৩ বার মনোযোগ দিয়ে পড়ো",
                    "২. উপমা, রূপক ও দৃষ্টান্ত বাদ দাও",
                    "৩. মূল বক্তব্য ৩ থেকে ৪টি স্পষ্ট বাক্যে লেখো",
                    "৪. উত্তম পুরুষ (আমি/আমরা) বা মধ্যম পুরুষ পরিহার করো",
                    "৫. নিজের কোনো নতুন মতামত যোগ করবে না"
                ),
                tips = listOf(
                    "মূল অনুচ্ছেদের এক-তৃতীয়াংশের বেশি যেন না হয়।",
                    "সহজ, প্রাঞ্জল ও গতিশীল বাক্য ব্যবহার করো।"
                )
            ),
            QuickRevisionItem(
                id = "rev-bangla-bhab",
                title = "ভাবসম্প্রসারণ লেখার নিয়ম",
                category = "ভাবসম্প্রসারণ",
                language = "Bengali",
                iconEmoji = "💡",
                keyStructure = listOf(
                    "১. মূলভাব: মূল উক্তির গভীর অর্থ সংক্ষেপে ২-৩ বাক্যে তুলে ধরা",
                    "২. সম্প্রসারিত ভাব: যুক্তি, সামাজিক প্রেক্ষাপট ও বাস্তব জীবনের উদাহরণের সাহায্যে বিস্তার",
                    "৩. উপসংহার: উক্তির সামগ্রিক শিক্ষা ও নৈতিক বার্তা"
                ),
                tips = listOf(
                    "একই কথার পুনরাবৃত্তি করবে না।",
                    "তিনটি সুস্পষ্ট অনুচ্ছেদে বিন্যস্ত রাখা সুন্দর।"
                )
            ),
            QuickRevisionItem(
                id = "rev-eng-app",
                title = "English Formal Application",
                category = "Application",
                language = "English",
                iconEmoji = "📝",
                keyStructure = listOf(
                    "1. Date: e.g., 15 March 2026",
                    "2. Designation & Address: The Headmaster / Principal",
                    "3. Subject: Application for [Clear purpose]",
                    "4. Salutation: Sir / Madam,",
                    "5. Opening: With due respect and humble submission...",
                    "6. Body: Clear cause, duration or required assistance",
                    "7. Request Conclusion: I, therefore, pray and hope that...",
                    "8. Subscription: Yours obediently / faithfully, Name, Class, Roll"
                ),
                tips = listOf(
                    "Keep layout left-aligned throughout.",
                    "Ensure correct punctuation in date and address.",
                    "Be concise and respectful."
                )
            ),
            QuickRevisionItem(
                id = "rev-eng-letter",
                title = "English Informal Letter",
                category = "Letter",
                language = "English",
                iconEmoji = "✉️",
                keyStructure = listOf(
                    "1. Heading: Sender's address and date (top-right or top-left)",
                    "2. Salutation: Dear [Friend's Name],",
                    "3. Opening: Take my warm love / Hope this letter finds you well",
                    "4. Body Paragraphs: Primary message, details and advice",
                    "5. Closing Paragraph: Convey my regards to your parents",
                    "6. Complimentary Close: Ever yours / With love,",
                    "7. Signature & Name",
                    "8. Envelope drawing with 'From' and 'To' addresses"
                ),
                tips = listOf(
                    "Keep tone friendly, warm and natural.",
                    "Always draw an envelope box with postage stamp."
                )
            ),
            QuickRevisionItem(
                id = "rev-eng-email",
                title = "English Email Writing",
                category = "Email",
                language = "English",
                iconEmoji = "📧",
                keyStructure = listOf(
                    "1. To: recipient@example.com",
                    "2. Subject: Short, clear, and informative",
                    "3. Salutation: Dear [Name], / Respected Sir,",
                    "4. Opening Sentence: Reason for writing",
                    "5. Core Content: Bulleted or 1-2 focused paragraphs",
                    "6. Call to Action / Friendly Wrap-up",
                    "7. Sign-off: Best regards, / Sincerely,",
                    "8. Sender's Name"
                ),
                tips = listOf(
                    "Never leave the Subject line empty.",
                    "Keep sentences crisp and avoid oversized walls of text."
                )
            ),
            QuickRevisionItem(
                id = "rev-eng-paragraph",
                title = "English Paragraph Structure",
                category = "Paragraph",
                language = "English",
                iconEmoji = "📄",
                keyStructure = listOf(
                    "1. Topic Sentence: Introduces the central idea of the paragraph",
                    "2. Supporting Sentences (3-5): Develop the idea with facts, causes, and impacts",
                    "3. Transition Words: Furthermore, In addition, Consequently, On the other hand",
                    "4. Concluding Sentence: Summarizes main point and provides a final thought"
                ),
                tips = listOf(
                    "A paragraph must have unity: focus on ONE central theme.",
                    "Avoid breaking into multiple sub-paragraphs unless writing an essay."
                )
            ),
            QuickRevisionItem(
                id = "rev-eng-report",
                title = "English Report Writing",
                category = "Report",
                language = "English",
                iconEmoji = "📰",
                keyStructure = listOf(
                    "1. Catchy Headline: Centered or bold",
                    "2. Byline: By a Staff Reporter / By [Student Name], Class X",
                    "3. Place and Date: Dhaka, 12 March 2026",
                    "4. Lead Paragraph: What happened, when, where, and who participated",
                    "5. Body: Sequence of events, speeches, highlights",
                    "6. Conclusion: Closing remarks and future outlook"
                ),
                tips = listOf(
                    "Use past tense for reporting completed events.",
                    "Maintain objective, factual reporting."
                )
            )
        )
    }
}
