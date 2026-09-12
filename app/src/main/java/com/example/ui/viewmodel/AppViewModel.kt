package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.content.ContentRepository
import com.example.data.database.AppDatabase
import com.example.data.database.entities.BookmarkEntity
import com.example.data.database.entities.RecentContentEntity
import com.example.data.database.entities.WritingDraftEntity
import com.example.data.model.CategoryMeta
import com.example.data.model.ContentDetail
import com.example.data.model.QuickRevisionItem
import com.example.data.model.SearchResult
import com.example.data.model.WritingItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val bookmarkDao = db.bookmarkDao()
    private val recentDao = db.recentContentDao()
    private val draftDao = db.writingDraftDao()
    private val contentRepository = ContentRepository(application)

    val allItems: StateFlow<List<WritingItem>> = contentRepository.items
    val isIndexed: StateFlow<Boolean> = contentRepository.isIndexed

    val bookmarks: StateFlow<List<BookmarkEntity>> = bookmarkDao.getAllBookmarks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentContents: StateFlow<List<RecentContentEntity>> = recentDao.getRecentContents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val drafts: StateFlow<List<WritingDraftEntity>> = draftDao.getAllDrafts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Reader UI state
    private val _currentDetail = MutableStateFlow<ContentDetail?>(null)
    val currentDetail: StateFlow<ContentDetail?> = _currentDetail.asStateFlow()

    private val _isLoadingDetail = MutableStateFlow(false)
    val isLoadingDetail: StateFlow<Boolean> = _isLoadingDetail.asStateFlow()

    private val _isExamView = MutableStateFlow(false)
    val isExamView: StateFlow<Boolean> = _isExamView.asStateFlow()

    private val _readerFontSize = MutableStateFlow(16.5f)
    val readerFontSize: StateFlow<Float> = _readerFontSize.asStateFlow()

    private val _readerLineSpacing = MutableStateFlow(1.5f)
    val readerLineSpacing: StateFlow<Float> = _readerLineSpacing.asStateFlow()

    // Search state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchLanguage = MutableStateFlow("All")
    val searchLanguage: StateFlow<String> = _searchLanguage.asStateFlow()

    private val _searchResults = MutableStateFlow<List<SearchResult>>(emptyList())
    val searchResults: StateFlow<List<SearchResult>> = _searchResults.asStateFlow()

    // Settings / Preferences
    private val _themeMode = MutableStateFlow("System") // "System", "Light", "Dark"
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    init {
        viewModelScope.launch {
            contentRepository.initializeIndex()
        }
    }

    fun getCategories(): List<CategoryMeta> {
        return contentRepository.getAllCategories()
    }

    fun getQuickRevisionItems(): List<QuickRevisionItem> {
        return contentRepository.getQuickRevisionItems()
    }

    fun isItemBookmarked(id: String): Boolean {
        return bookmarks.value.any { it.id == id }
    }

    fun toggleBookmark(item: WritingItem) {
        viewModelScope.launch {
            if (isItemBookmarked(item.id)) {
                bookmarkDao.deleteById(item.id)
            } else {
                bookmarkDao.insert(
                    BookmarkEntity(
                        id = item.id,
                        title = item.title,
                        category = item.category,
                        language = item.language,
                        assetPath = item.assetPath
                    )
                )
            }
        }
    }

    fun openContentDetail(itemId: String) {
        val item = contentRepository.getItemById(itemId)
        if (item == null) return

        viewModelScope.launch {
            _isLoadingDetail.value = true
            val detail = contentRepository.loadFullContent(item)
            _currentDetail.value = detail
            _isLoadingDetail.value = false

            // Record in recent contents
            recentDao.insertOrUpdate(
                RecentContentEntity(
                    id = item.id,
                    title = item.title,
                    category = item.category,
                    language = item.language,
                    assetPath = item.assetPath
                )
            )
        }
    }

    fun toggleExamView() {
        _isExamView.value = !_isExamView.value
    }

    fun setReaderFontSize(size: Float) {
        _readerFontSize.value = size.coerceIn(13f, 26f)
    }

    fun setReaderLineSpacing(factor: Float) {
        _readerLineSpacing.value = factor.coerceIn(1.2f, 2.0f)
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        executeSearch()
    }

    fun onSearchLanguageFilterChanged(lang: String) {
        _searchLanguage.value = lang
        executeSearch()
    }

    private fun executeSearch() {
        viewModelScope.launch {
            val q = _searchQuery.value
            val lang = _searchLanguage.value
            val results = contentRepository.searchContent(q, lang)
            _searchResults.value = results
        }
    }

    fun setThemeMode(mode: String) {
        _themeMode.value = mode
    }

    fun clearRecentHistory() {
        viewModelScope.launch {
            recentDao.clearAll()
        }
    }

    fun clearAllDrafts() {
        viewModelScope.launch {
            draftDao.clearAllDrafts()
        }
    }

    // Practice Mode State & Operations
    suspend fun getDraftForTopic(topicId: String): WritingDraftEntity? {
        return draftDao.getDraftForTopic(topicId)
    }

    fun saveDraft(draft: WritingDraftEntity) {
        viewModelScope.launch {
            draftDao.saveDraft(draft)
        }
    }

    fun deleteDraft(draftId: String) {
        viewModelScope.launch {
            draftDao.deleteDraftById(draftId)
        }
    }

    fun getItemById(id: String): WritingItem? {
        return contentRepository.getItemById(id)
    }
}
