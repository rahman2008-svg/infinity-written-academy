package com.example.ui.practice

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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.data.database.entities.WritingDraftEntity
import com.example.data.model.ContentDetail
import com.example.data.model.WritingItem
import com.example.ui.components.MarkdownRenderer
import com.example.ui.viewmodel.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PracticeScreen(
    initialItemId: String?,
    viewModel: AppViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val allItems by viewModel.allItems.collectAsState()

    var selectedItem by remember { mutableStateOf<WritingItem?>(null) }
    var loadedDetail by remember { mutableStateOf<ContentDetail?>(null) }
    var userText by remember { mutableStateOf("") }
    var isDropdownExpanded by remember { mutableStateOf(false) }
    var showCompareSheet by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    var lastSavedMessage by remember { mutableStateOf("") }

    // Initialize item selection
    LaunchedEffect(initialItemId, allItems) {
        if (allItems.isNotEmpty()) {
            val item = if (!initialItemId.isNullOrBlank()) {
                allItems.firstOrNull { it.id == initialItemId } ?: allItems.first()
            } else {
                selectedItem ?: allItems.first()
            }
            selectedItem = item

            // Check if draft exists
            val existingDraft = viewModel.getDraftForTopic(item.id)
            if (existingDraft != null) {
                userText = existingDraft.userText
                lastSavedMessage = "পূর্বে সংরক্ষিত ড্রাফট লোড করা হয়েছে"
            }
        }
    }

    // Load full detail when selected item changes
    LaunchedEffect(selectedItem) {
        selectedItem?.let { item ->
            val existingDraft = viewModel.getDraftForTopic(item.id)
            if (existingDraft != null) {
                userText = existingDraft.userText
            }
            // Trigger load detail
            viewModel.openContentDetail(item.id)
        }
    }

    val currentDetail by viewModel.currentDetail.collectAsState()
    LaunchedEffect(currentDetail) {
        if (currentDetail != null && currentDetail?.item?.id == selectedItem?.id) {
            loadedDetail = currentDetail
        }
    }

    val charCount = userText.length
    val wordCount = if (userText.isBlank()) 0 else userText.trim().split(Regex("\\s+")).size

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "হাতের লেখা অনুশীলন রুম",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Writing Practice & Self Evaluation",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
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
                    IconButton(
                        onClick = {
                            selectedItem?.let { item ->
                                val draft = WritingDraftEntity(
                                    id = "draft_${item.id}",
                                    topicId = item.id,
                                    topicTitle = item.title,
                                    language = item.language,
                                    category = item.category,
                                    userText = userText,
                                    lastModified = System.currentTimeMillis()
                                )
                                viewModel.saveDraft(draft)
                                lastSavedMessage = "ড্রাফট সফলভাবে সংরক্ষিত হয়েছে"
                                Toast.makeText(context, "ড্রাফট সংরক্ষিত হয়েছে", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.testTag("practice_save_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = "Save Draft",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = { showClearDialog = true },
                        modifier = Modifier.testTag("practice_clear_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Clear",
                            tint = MaterialTheme.colorScheme.outline
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            // Topic Picker Dropdown
            ExposedDropdownMenuBox(
                expanded = isDropdownExpanded,
                onExpandedChange = { isDropdownExpanded = !isDropdownExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = selectedItem?.title ?: "বিষয় নির্বাচন করুন",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("অনুশীলনের বিষয় (Selected Topic)") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDropdownExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                        .testTag("practice_topic_selector"),
                    shape = RoundedCornerShape(10.dp)
                )

                ExposedDropdownMenu(
                    expanded = isDropdownExpanded,
                    onDismissRequest = { isDropdownExpanded = false }
                ) {
                    allItems.forEach { item ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${item.category} • ${item.language}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            },
                            onClick = {
                                selectedItem = item
                                isDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Topic Question Prompt Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "প্রশ্ন / বিষয়বস্তু:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (!loadedDetail?.questionTopic.isNullOrBlank()) {
                            loadedDetail!!.questionTopic
                        } else {
                            selectedItem?.summary ?: "এই বিষয়ে আপনার নিজের ভাষায় লিখুন..."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Live Word & Char Counter
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                    ) {
                        Text(
                            text = "শব্দ: $wordCount",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                    ) {
                        Text(
                            text = "অক্ষর: $charCount",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                if (lastSavedMessage.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "সংরক্ষিত",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Practice Text Input
            OutlinedTextField(
                value = userText,
                onValueChange = { userText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("practice_input_field"),
                placeholder = {
                    Text(
                        text = "এখানে আপনার উত্তর লিখুন... (খাতা বা পরীক্ষার মতো গুছিয়ে পয়েন্ট ও অনুচ্ছেদ আকারে লিখুন)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                },
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Compare With Model Answer Button
            Button(
                onClick = { showCompareSheet = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("practice_compare_btn"),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Compare,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "আদর্শ উত্তরের সাথে তুলনা করুন (Compare Answer)",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    // Compare Bottom Sheet
    if (showCompareSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showCompareSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "উত্তর মূল্যায়ন ও তুলনা (Self-Check)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "আপনার লেখা উত্তর (${wordCount} শব্দ):",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (userText.isBlank()) "[কোনো লেখা নেই]" else userText,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "আদর্শ মডেল উত্তর (Standard Model Answer):",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(8.dp))

                val modelAnswerText = loadedDetail?.modelAnswer ?: loadedDetail?.rawMarkdown ?: "মডেল উত্তর লোড হচ্ছে..."
                MarkdownRenderer(
                    markdown = modelAnswerText,
                    baseFontSizeSp = 15f
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Clear Confirmation Dialog
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("লেখা মুছে ফেলতে চান?") },
            text = { Text("এই বিষয়ের জন্য আপনার লেখা সমস্ত টেক্সট পরিষ্কার হয়ে যাবে।") },
            confirmButton = {
                TextButton(
                    onClick = {
                        userText = ""
                        selectedItem?.let { item ->
                            viewModel.deleteDraft("draft_${item.id}")
                        }
                        showClearDialog = false
                        Toast.makeText(context, "পরিষ্কার করা হয়েছে", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("মুছে ফেলুন", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("বাতিল")
                }
            }
        )
    }
}
