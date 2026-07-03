package com.example.ui.chat

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.local.MessageEntity
import java.text.SimpleDateFormat
import java.util.*

fun formatLastSeen(lastSeen: Long): String {
    if (lastSeen == 0L) return "نشط منذ فترة"
    val diff = System.currentTimeMillis() - lastSeen
    if (diff < 0) return "متصل الآن"
    val minutes = diff / 1000 / 60
    return when {
        minutes < 1 -> "نشط منذ ثوانٍ"
        minutes < 60 -> "نشط منذ $minutes دقيقة"
        minutes < 1440 -> "نشط منذ ${minutes / 60} ساعة"
        else -> {
            val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
            "آخر ظهور ${sdf.format(Date(lastSeen))}"
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    currentUserId: String,
    onNavigateBack: () -> Unit,
    onNavigateToProfile: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    
    // Voice recording states
    var isRecording by remember { mutableStateOf(false) }
    var recordingTime by remember { mutableStateOf(0) }

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { uiState.peerUser?.id?.let { onNavigateToProfile(it) } }
                            .padding(vertical = 4.dp, horizontal = 2.dp)
                    ) {
                        Box {
                            AsyncImage(
                                model = uiState.peerUser?.photoUrl?.ifEmpty { "https://api.dicebear.com/7.x/avataaars/svg?seed=${uiState.peerUser?.displayName ?: "User"}" } ?: "https://api.dicebear.com/7.x/avataaars/svg?seed=User",
                                contentDescription = null,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentScale = ContentScale.Crop
                            )
                            if (uiState.peerUser?.isOnline == true) {
                                Box(
                                    modifier = Modifier
                                        .size(11.dp)
                                        .align(Alignment.BottomEnd)
                                        .background(Color(0xFF22C55E), CircleShape)
                                        .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
                                )
                            }
                        }
                        Column(horizontalAlignment = Alignment.Start) {
                            Text(
                                text = uiState.peerUser?.displayName ?: "محادثة",
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val statusText = when {
                                uiState.peerTypingState == "TYPING" -> "يكتب الآن..."
                                uiState.peerTypingState == "RECORDING" -> "يسجل رسالة صوتية..."
                                uiState.peerUser?.isOnline == true -> "متصل الآن"
                                else -> uiState.peerUser?.lastSeen?.let { formatLastSeen(it) } ?: "غير متصل"
                            }
                            val statusColor = if (uiState.peerTypingState != "IDLE" || uiState.peerUser?.isOnline == true) {
                                Color(0xFF22C55E)
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            }
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.labelSmall,
                                color = statusColor
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Simulated Call */ }) {
                        Icon(Icons.Default.Call, contentDescription = "اتصال صوتي", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { /* Simulated Video Call */ }) {
                        Icon(Icons.Default.Videocam, contentDescription = "اتصال مرئي", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            if (isRecording) {
                LaunchedEffect(Unit) {
                    recordingTime = 0
                    while (isRecording) {
                        kotlinx.coroutines.delay(1000)
                        recordingTime++
                    }
                }
                Surface(tonalElevation = 8.dp, color = MaterialTheme.colorScheme.surface) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(Color.Red, CircleShape)
                            )
                            val minutes = recordingTime / 60
                            val seconds = recordingTime % 60
                            Text(
                                text = String.format(Locale.US, "%02d:%02d", minutes, seconds),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        
                        Text(
                            text = "جاري تسجيل رسالة صوتية...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TextButton(
                                onClick = {
                                    isRecording = false
                                    recordingTime = 0
                                    viewModel.setTypingState("IDLE")
                                }
                            ) {
                                Text("إلغاء", color = MaterialTheme.colorScheme.error)
                            }
                            IconButton(
                                onClick = {
                                    val duration = String.format(Locale.US, "%02d:%02d", recordingTime / 60, recordingTime % 60)
                                    viewModel.sendMessage(duration, "VOICE")
                                    isRecording = false
                                    recordingTime = 0
                                },
                                modifier = Modifier.background(MaterialTheme.colorScheme.primary, shape = CircleShape)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "إرسال", tint = MaterialTheme.colorScheme.onPrimary)
                            }
                        }
                    }
                }
            } else {
                Surface(tonalElevation = 8.dp, color = MaterialTheme.colorScheme.surface) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { 
                                // Simulated File attachment sending
                                viewModel.sendMessage("تقرير_المشروع_النهائي.pdf|1.4 MB", "FILE")
                            }
                        ) {
                            Icon(Icons.Default.AttachFile, contentDescription = "إرفاق ملف", tint = MaterialTheme.colorScheme.primary)
                        }

                        IconButton(
                            onClick = { 
                                // Simulated Image attachment sending
                                viewModel.sendMessage("https://images.unsplash.com/photo-1506744038136-46273834b3fb?auto=format&fit=crop&w=600&q=80", "IMAGE")
                            }
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = "كاميرا", tint = MaterialTheme.colorScheme.primary)
                        }

                        OutlinedTextField(
                            value = messageText,
                            onValueChange = { 
                                messageText = it 
                                viewModel.updateTypingState(it.isNotBlank())
                            },
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 4.dp),
                            placeholder = { Text("اكتب رسالة...") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.SentimentSatisfied,
                                    contentDescription = "رموز تعبيرية",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            },
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                            singleLine = false,
                            maxLines = 4,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                            ),
                            shape = RoundedCornerShape(24.dp)
                        )
                        
                        Spacer(modifier = Modifier.width(4.dp))

                        if (messageText.isNotBlank()) {
                            IconButton(
                                onClick = {
                                    viewModel.sendMessage(messageText)
                                    messageText = ""
                                    viewModel.updateTypingState(false)
                                },
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(MaterialTheme.colorScheme.primary, shape = CircleShape)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "إرسال", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp))
                            }
                        } else {
                            IconButton(
                                onClick = { 
                                    isRecording = true
                                    viewModel.setTypingState("RECORDING")
                                },
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(MaterialTheme.colorScheme.primaryContainer, shape = CircleShape)
                            ) {
                                Icon(Icons.Default.Mic, contentDescription = "تسجيل صوتي", tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(uiState.messages) { message ->
                    val isMine = message.senderId == currentUserId
                    MessageBubble(message = message, isMine = isMine)
                }
            }
        }
    }
}

@Composable
fun MessageBubble(message: MessageEntity, isMine: Boolean) {
    val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    val timeString = timeFormat.format(Date(message.timestamp))
    var isAnimated by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isAnimated = true
    }

    AnimatedVisibility(
        visible = isAnimated,
        enter = fadeIn(animationSpec = spring()) + scaleIn(initialScale = 0.92f, animationSpec = spring())
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            horizontalArrangement = if (isMine) Arrangement.Start else Arrangement.End
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isMine) 4.dp else 16.dp,
                            bottomEnd = if (isMine) 16.dp else 4.dp
                        )
                    )
                    .background(
                        if (isMine) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                when (message.type) {
                    "VOICE" -> {
                        VoiceBubbleContent(duration = message.content, isMine = isMine)
                    }
                    "IMAGE" -> {
                        ImageBubbleContent(imageUrl = message.content)
                    }
                    "FILE" -> {
                        FileBubbleContent(fileData = message.content, isMine = isMine)
                    }
                    else -> {
                        // TEXT
                        Text(
                            text = message.content,
                            color = if (isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = timeString,
                        color = if (isMine) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.labelSmall
                    )
                    if (isMine) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = when (message.status) {
                                "SENT" -> "✓"
                                "DELIVERED" -> "✓✓"
                                "READ" -> "✓✓"
                                else -> ""
                            },
                            color = if (message.status == "READ") Color(0xFF60A5FA) else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VoiceBubbleContent(duration: String, isMine: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        val iconColor = if (isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
        val waveColor = if (isMine) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
        
        IconButton(
            onClick = { /* Simulated Voice Playback */ },
            modifier = Modifier
                .size(32.dp)
                .background(if (isMine) Color.White.copy(alpha = 0.15f) else MaterialTheme.colorScheme.primaryContainer, CircleShape)
        ) {
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = "تشغيل الصوت",
                tint = iconColor,
                modifier = Modifier.size(18.dp)
            )
        }
        
        // Render stylized Voice Waveform inside Chat
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val heights = listOf(8, 16, 24, 12, 18, 6, 14, 22, 10, 18, 14, 8, 20, 12, 6)
            heights.forEach { heightVal ->
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(heightVal.dp)
                        .background(waveColor, RoundedCornerShape(1.5.dp))
                )
            }
        }
        
        Text(
            text = duration,
            color = if (isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
fun ImageBubbleContent(imageUrl: String) {
    AsyncImage(
        model = imageUrl,
        contentDescription = "صورة مرفقة",
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(RoundedCornerShape(8.dp)),
        contentScale = ContentScale.Crop
    )
}

@Composable
fun FileBubbleContent(fileData: String, isMine: Boolean) {
    val parts = fileData.split("|")
    val fileName = parts.getOrNull(0) ?: "file.pdf"
    val fileSize = parts.getOrNull(1) ?: "1.4 MB"
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isMine) Color.White.copy(alpha = 0.12f)
                else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                RoundedCornerShape(12.dp)
            )
            .padding(10.dp)
    ) {
        Icon(
            Icons.Default.InsertDriveFile,
            contentDescription = null,
            tint = if (isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(28.dp)
        )
        Column {
            Text(
                text = fileName,
                fontWeight = FontWeight.Bold,
                color = if (isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "$fileSize • PDF",
                color = if (isMine) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}
