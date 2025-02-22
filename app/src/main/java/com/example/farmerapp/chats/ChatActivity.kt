package com.example.farmerapp.chats

import android.app.Activity
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LiveData
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.example.farmerapp.R
import com.example.farmerapp.ui.theme.FarmerAppTheme
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth.getInstance
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Locale


data class ChatMessage(
    val message: String = "",
    val senderId: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val farmerId: String = ""
)

class ChatActivity : ComponentActivity() {

    private val chatViewModel: ChatViewModel by viewModels()
    private val chatHistoryViewModel: ChatHistoryViewModel by viewModels()
    private lateinit var userId: String
    private lateinit var friendId: String
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        userId = intent.getStringExtra("userId") ?: ""
        friendId = intent.getStringExtra("friendId") ?: ""

        if (userId.isEmpty() || friendId.isEmpty()) {
            Log.e("ChatActivity", "Invalid userId or friendId")
            finish()
            return
        }

        firestore = FirebaseFirestore.getInstance()

        lifecycleScope.launch {
            try {
                chatViewModel.checkOrCreateChat(userId, friendId) { chatId ->
                    lifecycleScope.launch {
                        chatHistoryViewModel.loadProfilePictures(
                            listOf(userId, friendId).distinct()
                        )
                        chatViewModel.fetchChatThread(chatId)
                        chatViewModel.loadChatMessages(chatId)
                        chatViewModel.listenForMessages(chatId)
                    }

                    // Set the content with the friend's name
                    setContent {
                        FarmerAppTheme {
                            ChatScaffold(
                                userId = userId,
                                friendId = friendId,
                                chatMessages = chatViewModel.chatMessages,
                                // Pass chat thread
                                sendMessage = { message ->
                                    chatViewModel.sendMessage(chatId, message)
                                }
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("ChatActivity", "Error", e)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        chatViewModel.onCleared()
    }
}


@Composable
fun ChatScaffold(
    userId: String,
    friendId: String,
    chatMessages: LiveData<List<ChatMessage>>,
    sendMessage: (ChatMessage) -> Unit
) {
    val activity = LocalContext.current as? Activity

    Scaffold(
        content = { paddingValues ->
            ChatScreen(
                userId = userId,
                friendId = friendId,
                chatMessages = chatMessages,
                sendMessage = sendMessage,
                onBackClick = { activity?.finish() },
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(paddingValues)
            )
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    userId: String,
    friendId: String,
    chatMessages: LiveData<List<ChatMessage>>,
    sendMessage: (ChatMessage) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val chatHistoryViewModel: ChatHistoryViewModel = viewModel()
    var messageText by remember { mutableStateOf("") }
    val messages by chatMessages.observeAsState(emptyList())
    val lazyListState = rememberLazyListState()
    val firestore = FirebaseFirestore.getInstance()
    val currentId: String = getInstance().currentUser?.uid ?: ""


    val profilePictures by chatHistoryViewModel.profilePictures.observeAsState(emptyMap()) // Assuming you fetch profile pictures
    val userIdToDisplay = if (currentId == friendId) userId else friendId
    val profilePictureUrl = profilePictures[userIdToDisplay]
    val painter = rememberAsyncImagePainter(
        model = profilePictureUrl,
        placeholder = painterResource( R.drawable.img),
        error = painterResource(R.drawable.art)
    )



    Column(
        modifier = modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colorScheme.surface)
            .padding(8.dp)
    ) {

        // State for storing user names
        var userName by remember { mutableStateOf("") }
        var friendName by remember { mutableStateOf("") }

        // Fetch names once when the composable is launched
        LaunchedEffect(key1 = currentId) {
            try {
                val userNames = firestore.collection("users")
                    .whereIn(FieldPath.documentId(), listOf(userId, friendId))
                    .get()
                    .await()
                    .documents
                    .associateBy({ it.id }, { it.getString("name") ?: "Unknown" })

                userName = userNames[userId] ?: "Unknown"
                friendName = userNames[friendId] ?: "Unknown"
            } catch (e: Exception) {
                Log.e("ChatScreen", "Error fetching user names", e)
                userName = "Error"
                friendName = "Error"
            }
        }

        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    ) {
                        Image(
                            painter = painter,
                            contentDescription = "Profile Picture",
                            alignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = if (currentId == userId) friendName else userName)
                }
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        )


                LazyColumn(
            state = lazyListState,
            modifier = Modifier.weight(1f),
        ) {
            items(messages) { message ->
                MessageBubble(
                    chatMessage = message,
                    isSentByCurrentUser = message.senderId == currentId
                )
            }
        }

        MessageInputField(
            message = messageText,
            onMessageChange = { messageText = it },
            onSendClick = {
                if (messageText.isNotBlank()) {
                    val newMessage = ChatMessage(
                        message = messageText,
                        senderId = currentId,
                        timestamp = Timestamp.now(),
                        farmerId = friendId
                    )
                    sendMessage(newMessage)
                    messageText = ""
                }
            }
        )
    }
}

@Composable
fun MessageBubble(chatMessage: ChatMessage, isSentByCurrentUser: Boolean) {
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val backgroundColor = if (isSentByCurrentUser) {
        MaterialTheme.colorScheme.secondary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    val textColor = if (isSentByCurrentUser) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onBackground
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 8.dp),
        horizontalArrangement = if (isSentByCurrentUser) Arrangement.End else Arrangement.Start
    ) {
        Column(
            horizontalAlignment = if (isSentByCurrentUser) Alignment.End else Alignment.Start
        ) {
            // Message bubble
            Surface(
                color = backgroundColor,
                shape = MaterialTheme.shapes.medium.copy(
                    topEnd = if (isSentByCurrentUser) CornerSize(0.dp) else MaterialTheme.shapes.medium.topEnd,
                    topStart = if (!isSentByCurrentUser) CornerSize(0.dp) else MaterialTheme.shapes.medium.topStart,
                    bottomEnd = CornerSize(16.dp),
                    bottomStart = CornerSize(16.dp)
                ),
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .widthIn(min = 50.dp, max = 250.dp)
                    .wrapContentWidth()
            ) {
                Text(
                    text = chatMessage.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor,
                    modifier = Modifier.padding(vertical = 6.dp, horizontal = 12.dp)
                )
            }
            Text(
                text = timeFormat.format(chatMessage.timestamp.toDate()),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier
                    .padding(top = 2.dp, end = 8.dp, start = 8.dp)
                    .align(if (isSentByCurrentUser) Alignment.End else Alignment.Start)
            )

        }
    }
}

@Composable
fun MessageInputField(
    message: String,
    onMessageChange: (String) -> Unit,
    onSendClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium)
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        BasicTextField(
            value = message,
            onValueChange = onMessageChange,
            modifier = Modifier
                .weight(1f)
                .background(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.large)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            maxLines = 4,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
            decorationBox = { innerTextField ->
                if (message.isEmpty()) {
                    Text(
                        text = "Type a message",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
                innerTextField()
            }
        )
        IconButton(
            onClick = onSendClick,
            enabled = message.isNotBlank(),
            modifier = Modifier
                .padding(start = 8.dp)
                .size(48.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send, // WhatsApp-like paper plane icon
                contentDescription = "Send Message",
                tint = if (message.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant // WhatsApp green
            )
        }
    }
}
