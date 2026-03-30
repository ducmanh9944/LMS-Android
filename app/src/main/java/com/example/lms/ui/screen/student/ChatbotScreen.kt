package com.example.lms.ui.screen.student

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.lms.data.model.ChatMessage
import com.example.lms.data.model.ChatSender
import com.example.lms.data.model.ChatSession
import com.example.lms.ui.component.TopBar
import com.example.lms.util.ChatbotEvent
import com.example.lms.viewmodel.ChatbotViewModel

private val ChatBg = Color(0xFFF8FAFC)
private val ChatUserBubble = Color(0xFF4B5CC4)
private val ChatBotBubble = Color.White
private val ChatBorder = Color(0xFFE2E8F0)
private val ChatTextPrimary = Color(0xFF1E293B)
private val ChatTextSecondary = Color(0xFF64748B)
private val ChatInputBg = Color(0xFFF1F3F8)

@Composable
fun ChatbotRoute(
    userId: String,
    userAvatarUrl: String?,
    viewModel: ChatbotViewModel,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var input by remember { mutableStateOf("") }

    LaunchedEffect(userId) {
        viewModel.init(userId)
    }

    LaunchedEffect(Unit) {
        viewModel.event.collect { event ->
            if (event is ChatbotEvent.ShowError) {
                snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    ChatbotScreen(
        isLoading = uiState.isLoading,
        isSending = uiState.isSending,
        sessions = uiState.sessions,
        canCreateNewSession = uiState.canCreateNewSession,
        selectedSessionId = uiState.sessionId,
        messages = uiState.messages,
        userAvatarUrl = userAvatarUrl,
        input = input,
        snackbarHostState = snackbarHostState,
        onInputChange = { input = it },
        onSelectSession = viewModel::selectSession,
        onCreateSession = { viewModel.createNewSession(userId) },
        onSendClick = {
            if (input.isNotBlank()) {
                viewModel.sendMessage(input)
                input = ""
            }
        },
        onBackClick = onBackClick
    )
}

@Composable
private fun ChatbotScreen(
    isLoading: Boolean,
    isSending: Boolean,
    sessions: List<ChatSession>,
    canCreateNewSession: Boolean,
    selectedSessionId: String,
    messages: List<ChatMessage>,
    userAvatarUrl: String?,
    input: String,
    snackbarHostState: SnackbarHostState,
    onInputChange: (String) -> Unit,
    onSelectSession: (String) -> Unit,
    onCreateSession: () -> Unit,
    onSendClick: () -> Unit,
    onBackClick: () -> Unit
) {
    val listState = rememberLazyListState()

    LaunchedEffect(selectedSessionId, messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Scaffold(
        topBar = {
            TopBar(
                title = "Trợ lý học tập",
                onBackClick = onBackClick
            )
        },
        containerColor = ChatBg,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ChatBg)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .navigationBarsPadding()
                    .imePadding(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextField(
                    modifier = Modifier
                        .weight(1f),
                    value = input,
                    onValueChange = onInputChange,
                    shape = RoundedCornerShape(14.dp),
                    placeholder = { Text("Hỏi trợ lý...", color = ChatTextSecondary) },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = ChatInputBg,
                        unfocusedContainerColor = ChatInputBg,
                        disabledContainerColor = ChatInputBg,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    )
                )

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(ChatUserBubble)
                        .clickable(enabled = input.isNotBlank() && !isLoading && !isSending) {
                            onSendClick()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSending) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Gửi",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            SessionSelectorRow(
                sessions = sessions,
                canCreateNewSession = canCreateNewSession,
                selectedSessionId = selectedSessionId,
                onSelectSession = onSelectSession,
                onCreateSession = onCreateSession
            )

            Spacer(modifier = Modifier.height(4.dp))

            if (isLoading && messages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = ChatUserBubble)
                }
            } else if (messages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Bắt đầu cuộc trò chuyện để nhận hỗ trợ học tập.",
                        color = ChatTextSecondary,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    state = listState,
                    contentPadding = PaddingValues(bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(messages, key = { it.id }) { message ->
                        MessageBubble(
                            message = message,
                            userAvatarUrl = userAvatarUrl
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionSelectorRow(
    sessions: List<ChatSession>,
    canCreateNewSession: Boolean,
    selectedSessionId: String,
    onSelectSession: (String) -> Unit,
    onCreateSession: () -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(10.dp))
            .padding(horizontal = 6.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(sessions, key = { it.id }) { session ->
            val selected = session.id == selectedSessionId
            Surface(
                shape = RoundedCornerShape(9.dp),
                color = if (selected) Color(0xFFE9EDFF) else Color.White,
                onClick = { onSelectSession(session.id) },
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = if (selected) ChatUserBubble.copy(alpha = 0.4f) else ChatBorder
                ),
                modifier = Modifier.widthIn(min = 62.dp)
            ) {
                Text(
                    text = session.title.ifBlank { "Phiên trò chuyện" },
                    color = if (selected) ChatUserBubble else ChatTextPrimary,
                    fontSize = 10.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                    maxLines = 1
                )
            }
        }

        item {
            Surface(
                shape = RoundedCornerShape(9.dp),
                color = if (canCreateNewSession) Color(0xFFE9EDFF) else Color(0xFFF1F3F8),
                onClick = onCreateSession,
                enabled = canCreateNewSession,
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = if (canCreateNewSession) ChatUserBubble.copy(alpha = 0.35f) else ChatBorder
                )
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Tạo phiên mới",
                        tint = if (canCreateNewSession) ChatUserBubble else ChatTextSecondary,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(
    message: ChatMessage,
    userAvatarUrl: String?
) {
    val isUser = message.sender == ChatSender.USER

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (!isUser) {
            Surface(
                modifier = Modifier.size(30.dp),
                shape = CircleShape,
                color = Color(0xFFEDEBFF)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.SmartToy,
                        contentDescription = null,
                        tint = ChatUserBubble,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.size(8.dp))
        }

        Surface(
            color = if (isUser) ChatUserBubble else ChatBotBubble,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .widthIn(max = 280.dp)
                .border(
                    width = if (isUser) 0.dp else 1.dp,
                    color = if (isUser) Color.Transparent else ChatBorder,
                    shape = RoundedCornerShape(16.dp)
                )
        ) {
            Text(
                text = message.content,
                color = if (isUser) Color.White else ChatTextPrimary,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp)
            )
        }

        if (isUser) {
            Spacer(modifier = Modifier.size(8.dp))
            Surface(
                modifier = Modifier.size(30.dp),
                shape = CircleShape,
                color = Color.White,
                border = androidx.compose.foundation.BorderStroke(1.dp, ChatBorder)
            ) {
                if (!userAvatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = userAvatarUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = ChatTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

