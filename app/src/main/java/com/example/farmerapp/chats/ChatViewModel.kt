package com.example.farmerapp.chats
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.farmerapp.models.ChatThread
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ChatViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()

    private var messageListenerRegistration: ListenerRegistration? = null

    private val _chatMessages = MutableLiveData<List<ChatMessage>>(emptyList())
    val chatMessages: LiveData<List<ChatMessage>> = _chatMessages

    private val _chatThread = MutableLiveData<ChatThread>()

    private val _errorMessage = MutableLiveData<String?>(null)

    // Function to check or create chat
    fun checkOrCreateChat(userId: String, friendId: String, onChatIdReady: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val customCredential: String = if (userId >= friendId) {
                    "$userId-$friendId"
                } else {
                    "$friendId-$userId"
                }
                val chatDoc = firestore.collection("chats").document(customCredential).get().await()

                if (chatDoc.exists()) {
                    onChatIdReady(chatDoc.id)
                } else {
                    startNewChat(userId, friendId, onChatIdReady)
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error checking chat existence"
            }
        }
    }


    // Function to start a new chat
    private fun startNewChat(currentUserId: String, friendId: String, onChatCreated: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val customCredential : String = if (currentUserId >= friendId) {
                    "$currentUserId-$friendId"
                }else{
                    "$friendId-$currentUserId"
                }
                val chatId = firestore.collection("chats").document(customCredential).id


                val chatData = hashMapOf(
                    "lastMessage" to "",
                    "lastMessageTime" to Timestamp.now(),
                    "userId" to currentUserId,
                    "friendId" to friendId
                )

                firestore.collection("chats").document(chatId).set(chatData).await()

                // Update both users' chatIds
                firestore.collection("users").document(currentUserId)
                    .update("chatIds", FieldValue.arrayUnion(chatId)).await()

                firestore.collection("users").document(friendId)
                    .update("chatIds", FieldValue.arrayUnion(chatId)).await()

                onChatCreated(chatId)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to start chat"
            }
        }
    }

    fun listenForMessages(chatId: String) {
        messageListenerRegistration = firestore.collection("chats")
            .document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _errorMessage.value = "Error listening for messages"
                    return@addSnapshotListener
                }

                snapshot?.documentChanges?.forEach { change ->
                    if (change.type == DocumentChange.Type.ADDED) {
                        val message = change.document.toObject(ChatMessage::class.java)
                        _chatMessages.value = (_chatMessages.value ?: emptyList()) + message
                    }
                }

            }
    }

    // Function to load messages for a specific chat
    fun loadChatMessages(chatId: String) {
        viewModelScope.launch {
            try {
                val messages = firestore.collection("chats")
                    .document(chatId)
                    .collection("messages")
                    .orderBy("timestamp", Query.Direction.ASCENDING)
                    .get()
                    .await()
                    .documents
                    .map { it.toObject(ChatMessage::class.java)!! }

                _chatMessages.value = messages
            } catch (e: Exception) {
                _errorMessage.value = "Error loading messages"
            }
        }
    }

    // Function to send a new message
    fun sendMessage(chatId: String, message: ChatMessage) {
        viewModelScope.launch {
            try {
                firestore.collection("chats")
                    .document(chatId)
                    .collection("messages")
                    .add(message)
                    .await()

                try {
                    firestore.collection("chats").document(chatId)
                        .update("lastMessage", message.message, "lastMessageTime", message.timestamp)
                } catch (e: Exception) {
                    _errorMessage.value = "Error updating last message info"
                }

            } catch (e: Exception) {
                _errorMessage.value = "Error sending message"
            }
        }
    }


    fun  fetchChatThread(chatId: String) {
        firestore.collection("chats").document(chatId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val friendId = document.getString("friendId") ?: ""
                    val userId = document.getString("userId") ?: ""
                    val lastMessage = document.getString("lastMessage") ?: ""
                    val lastMessageTime = document.getTimestamp("lastMessageTime") ?: Timestamp.now()

                    val chatThread = ChatThread(
                        friendId = friendId,
                        userId = userId,
                        lastMessage = lastMessage,
                        lastMessageTime = lastMessageTime,
                        chatId = chatId
                    )

                    _chatThread.value = chatThread
                }
            }
            .addOnFailureListener { exception ->
                Log.e("ChatViewModel", "Error fetching chat thread", exception)
            }
    }

    public override fun onCleared() {
        super.onCleared()
        messageListenerRegistration?.remove() // Clean up Firestore listeners
    }
}
