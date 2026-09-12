package com.example.ui.reader

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TextFormat
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.MarkdownRenderer
import com.example.ui.viewmodel.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    itemId: String,
    viewModel: AppViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToPractice: (String) -> Unit
) {
    val context = LocalContext.current
    val detail by viewModel.currentDetail.collectAsState()
    val isLoading by viewModel.isLoadingDetail.collectAsState()
    val isExamView by viewModel.isExamView.collectAsState()
    val fontSize by viewModel.readerFontSize.collectAsState()
    val lineSpacing by viewModel.readerLineSpacing.collectAsState()
    val bookmarks by viewModel.bookmarks.collectAsState()

    var showFontSheet by remember { mutableStateOf(false) }
    var isSearchActive by remember { mutableStateOf(false) }
    var inPageSearchQuery by remember { mutableStateOf("") }

    val scrollState = rememberScrollState()

    LaunchedEffect(itemId) {
        viewModel.openContentDetail(itemId)
    }

    val isBookmarked = detail?.item?.let { item ->
        bookmarks.any { it.id == item.id }
    } ?: false

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearchActive) {
                        OutlinedTextField(
                            value = inPageSearchQuery,
                            onValueChange = { inPageSearchQuery = it },
                            placeholder = { Text("পৃষ্ঠায় শব্দ খুঁজুন...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("in_page_search_input"),
                            singleLine = true,
                            trailingIcon = {
                                IconButton(onClick = {
                                    if (inPageSearchQuery.isNotEmpty()) {
                                        inPageSearchQuery = ""
                                    } else {
                                        isSearchActive = false
                                    }
                                }) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close Search")
                                }
                            }
                        )
                    } else {
                        Column {
                            Text(
                                text = detail?.item?.title ?: "পঠন কক্ষ",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Text(
                                text = "${detail?.item?.category ?: ""} • ${if (isExamView) "পরীক্ষার মোড" else "নরমাল ভিউ"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (!isSearchActive) {
                        IconButton(
                            onClick = { isSearchActive = true },
                            modifier = Modifier.testTag("reader_search_btn")
                        ) {
                            Icon(imageVector = Icons.Default.Search, contentDescription = "Search in page")
                        }

                        // Exam View Toggle button
                        IconButton(
                            onClick = { viewModel.toggleExamView() },
                            modifier = Modifier.testTag("reader_exam_mode_btn")
                        ) {
                            Icon(
                                imageVector = if (isExamView) Icons.Default.Visibility else Icons.Outlined.VisibilityOff,
                                contentDescription = "Toggle Exam View",
                                tint = if (isExamView) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            )
                        }

                        // Font sizing bottom sheet button
                        IconButton(
                            onClick = { showFontSheet = true },
                            modifier = Modifier.testTag("reader_font_btn")
                        ) {
                            Icon(imageVector = Icons.Default.TextFormat, contentDescription = "Font Settings")
                        }

                        // Bookmark
                        IconButton(
                            onClick = {
                                detail?.item?.let { viewModel.toggleBookmark(it) }
                            },
                            modifier = Modifier.testTag("reader_bookmark_btn")
                        ) {
                            Icon(
                                imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = "Bookmark",
                                tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Copy action
                    IconButton(
                        onClick = {
                            detail?.rawMarkdown?.let { text ->
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("WrittenAcademy", text)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "লেখাটি ক্লিপবোর্ডে কপি করা হয়েছে", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy Content")
                    }

                    // Practice Room shortcut button
                    FilterChip(
                        selected = true,
                        onClick = {
                            detail?.item?.id?.let { onNavigateToPractice(it) }
                        },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Create,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "অনুশীলন রুমে লিখুন",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        },
                        modifier = Modifier.testTag("reader_practice_chip")
                    )
                }
            }
        }
    ) { innerPadding ->
        if (isLoading || detail == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            val current = detail!!
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(scrollState)
                    .padding(16.dp)
                    .testTag("reader_content_area")
            ) {
                // Exam Mode Banner
                if (isExamView) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "পরীক্ষার কুইক ভিউ মোড: মূল উত্তর ও কাঠামোর ওপর আলোকপাত করা হয়েছে।",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    // In Exam View: Render question, format, and model answer concisely
                    if (current.questionTopic.isNotEmpty()) {
                        Text(
                            text = "প্রশ্ন / Topic",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        MarkdownRenderer(
                            markdown = current.questionTopic,
                            baseFontSizeSp = fontSize,
                            lineHeightFactor = lineSpacing,
                            searchQuery = inPageSearchQuery
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    if (current.formatStructure.isNotEmpty()) {
                        Text(
                            text = "লেখার নিয়ম ও কাঠামো",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        MarkdownRenderer(
                            markdown = current.formatStructure,
                            baseFontSizeSp = fontSize,
                            lineHeightFactor = lineSpacing,
                            searchQuery = inPageSearchQuery
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    Text(
                        text = "আদর্শ উত্তর (Model Answer)",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    MarkdownRenderer(
                        markdown = current.modelAnswer,
                        baseFontSizeSp = fontSize,
                        lineHeightFactor = lineSpacing,
                        searchQuery = inPageSearchQuery
                    )
                } else {
                    // Full Normal View
                    MarkdownRenderer(
                        markdown = current.rawMarkdown,
                        baseFontSizeSp = fontSize,
                        lineHeightFactor = lineSpacing,
                        searchQuery = inPageSearchQuery
                    )
                }

                Spacer(modifier = Modifier.height(36.dp))
            }
        }
    }

    // Font Sizing & Typography Bottom Sheet
    if (showFontSheet) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { showFontSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "পঠন ও ফন্ট সেটিংস (Reading Preferences)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "ফন্টের আকার (${fontSize.toInt()}sp)")
                    Row {
                        IconButton(onClick = { viewModel.setReaderFontSize(fontSize - 1f) }) {
                            Text(text = "A-", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        IconButton(onClick = { viewModel.setReaderFontSize(fontSize + 1f) }) {
                            Text(text = "A+", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                    }
                }

                Slider(
                    value = fontSize,
                    onValueChange = { viewModel.setReaderFontSize(it) },
                    valueRange = 13f..26f,
                    steps = 12
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(text = "লাইন ব্যবধান (Line Spacing)")
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = lineSpacing <= 1.35f,
                        onClick = { viewModel.setReaderLineSpacing(1.3f) },
                        label = { Text("ঘন (Compact)") }
                    )
                    FilterChip(
                        selected = lineSpacing in 1.36f..1.6f,
                        onClick = { viewModel.setReaderLineSpacing(1.5f) },
                        label = { Text("স্বাভাবিক (Normal)") }
                    )
                    FilterChip(
                        selected = lineSpacing > 1.6f,
                        onClick = { viewModel.setReaderLineSpacing(1.8f) },
                        label = { Text("প্রশস্ত (Spacious)") }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
