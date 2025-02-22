package com.example.farmerapp.chats

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.example.farmerapp.R
import com.example.farmerapp.auth.SharedPreferencesHelper
import com.example.farmerapp.models.ChatThread
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth.getInstance
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun ChatHistoryScreen(currentUserId: String, modifier: Modifier = Modifier) {
    val chatHistoryViewModel: ChatHistoryViewModel = viewModel()
    val chats by chatHistoryViewModel.chats.observeAsState(emptyList())
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    SharedPreferencesHelper(context)

    // Invoke loadChatHistory
    LaunchedEffect(Unit) {
        chatHistoryViewModel.loadChatHistory(
            userId = currentUserId,
            onSuccess = { threads ->
                chatHistoryViewModel.updateChats(threads)
                chatHistoryViewModel.loadProfilePictures(
                    threads.flatMap { listOf(it.userId, it.friendId) }.distinct()
                )
                chatHistoryViewModel.loadUserNames(
                    threads.flatMap { listOf(it.userId, it.friendId) }.distinct()
                )
                isLoading = false
            },
            onFailure = { exception ->
                Log.e("ChatHistoryScreen", "Failed to load chat history: ${exception.message}")
                errorMessage = "Failed to load chat history."
                isLoading = false
            }
        )
    }

    DisposableEffect(Unit) {
        val db = FirebaseFirestore.getInstance()
        val listenerRegistrations = mutableListOf<ListenerRegistration>()

        chatHistoryViewModel.setupUserListener(
            db,
            currentUserId,
            listenerRegistrations,
            { errorMessage = it; isLoading = false },
            { chatIds ->
                if (chatIds.isNotEmpty()) {
                    chatIds.forEach { chatId ->
                        chatHistoryViewModel.setupChatListener(
                            db,
                            chatId,
                            listenerRegistrations,
                            { errorMessage = it; isLoading = false },
                            { chatThreads ->
                                chatThreads.forEach { chatHistoryViewModel.updateChat(it) }
                                isLoading = false
                            }
                        )
                    }
                } else {
                    isLoading = false
                }
            }
        )

        onDispose {
            listenerRegistrations.forEach { it.remove() }
        }
    }


    when {
        isLoading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        errorMessage != null -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(errorMessage!!, color = Color.Red)
            }
        }
        chats.isEmpty() -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("There's no conversation yet.", style = MaterialTheme.typography.bodyLarge)
            }
        }
        else -> {
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(chats) { thread ->
                    ChatHistoryCard(thread, context, chatHistoryViewModel)
                }
            }
        }
    }
}

@Composable
fun ChatHistoryCard(
    thread: ChatThread,
    context: Context,
    chatHistoryViewModel: ChatHistoryViewModel
) {
    val currentId: String = getInstance().currentUser?.uid ?: ""
    val userNames by chatHistoryViewModel.userNames.observeAsState(emptyMap())
    val profilePictures by chatHistoryViewModel.profilePictures.observeAsState(emptyMap())
    val userIdToDisplay = if (currentId == thread.friendId) thread.userId else thread.friendId
    val profilePictureUrl = profilePictures[userIdToDisplay]
    val painter = rememberAsyncImagePainter(
        model = profilePictureUrl ?: R.drawable.img,
        placeholder = painterResource( R.drawable.art),
        error = painterResource(R.drawable.art)
    )
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                // Start ChatActivity with farmerId and chatId
                val intent = Intent(context, ChatActivity::class.java).apply {
                    putExtra("userId", thread.userId)
                    putExtra("friendId", thread.friendId)
                }
                context.startActivity(intent)
                       },
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.Start
        ) {
             Box(
                 modifier = Modifier
                     .size(46.dp)
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
            Column {
                val userName = userNames[thread.userId] ?: "Loading..."
                val friendName = userNames[thread.friendId] ?: "Loading..."
                Text(
                    text = if (currentId == thread.friendId) userName else friendName,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(text = thread.lastMessage, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = getFormattedTime(thread.lastMessageTime),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.align(Alignment.CenterVertically)
            )
        }
    }
}

private fun getFormattedTime(lastMessageTime: Timestamp): String {
    val date = lastMessageTime.toDate()
    val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
    return sdf.format(date)
}

