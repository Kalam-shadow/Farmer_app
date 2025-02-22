package com.example.farmerapp.chats

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.farmerapp.models.ChatThread
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ChatHistoryViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()
    private val _chats = MutableLiveData<List<ChatThread>>()
    val chats: LiveData<List<ChatThread>> = _chats

    private val _userNames = MutableLiveData<Map<String, String>>()
    val userNames: LiveData<Map<String, String>> get() = _userNames

    private val _profilePictures = MutableLiveData<Map<String, String>>()
    val profilePictures: LiveData<Map<String, String>> get() = _profilePictures

    // Function to update the LiveData with the fetched chat threads
    fun updateChats(chatThreads: List<ChatThread>) {
        _chats.value = chatThreads
    }

    fun updateChat(chatThread: ChatThread) {
        val updatedList = _chats.value?.toMutableList() ?: mutableListOf()
        val index = updatedList.indexOfFirst { it.chatId == chatThread.chatId }
        if (index != -1) {
            updatedList[index] = chatThread
        } else {
            updatedList.add(chatThread)
        }
        // Sort the list by `lastMessageTime` in descending order
        updatedList.sortByDescending { it.lastMessageTime }
        _chats.value = updatedList
    }

    fun loadUserNames(userIds: List<String>) {
        if (userIds.isEmpty()) {
            Log.e("ChatHistoryViewModel", "User IDs list is empty. Skipping Firestore query.")
            // Optionally update _userNames to indicate no data
            _userNames.value = emptyMap()
            return
        }
        viewModelScope.launch {
            try {
                val firestore = FirebaseFirestore.getInstance()
                val userNames = firestore.collection("users")
                    .whereIn(FieldPath.documentId(), userIds)
                    .get()
                    .await()
                    .documents
                    .associateBy({ it.id }, { it.getString("name") ?: "Unknown" })
                _userNames.value = userNames
            } catch (e: Exception) {
                Log.e("ChatHistoryViewModel", "Error loading user names", e)
            }
        }
    }

    fun loadProfilePictures(userIds: List<String>){
        if (userIds.isEmpty()) {
            Log.e("ChatHistoryViewModel", "User IDs list is empty. Skipping Firestore query.")
            // Optionally update _userNames to indicate no data
            _profilePictures.value = emptyMap()
            return
        }
        viewModelScope.launch {
            try{
                val  firestore = FirebaseFirestore.getInstance()
                val profilePictures = firestore.collection("users")
                    .whereIn(FieldPath.documentId(),userIds)
                    .get()
                    .await()
                    .documents
                    .associateBy({it.id},{it.getString("profileImageUrl")?:""})
                _profilePictures.postValue(profilePictures)
            }catch (e:Exception){
                Log.e("ChatHistoryViewModel","Error loading profile pictures",e)
            }
        }
    }
    fun setupUserListener(
        db: FirebaseFirestore,
        currentUserId: String,
        listenerRegistrations: MutableList<ListenerRegistration>,
        onError: (String) -> Unit,
        onUpdateChats: (List<String>) -> Unit
    ): ListenerRegistration {
        val userRef = db.collection("users").document(currentUserId)
        return userRef.addSnapshotListener { documentSnapshot, e ->
            if (e != null) {
                Log.e("ChatHistoryScreen", "Failed to listen for user updates.", e)
                onError("Failed to load user data.")
                return@addSnapshotListener
            }
            if (documentSnapshot != null && documentSnapshot.exists()) {
                val chatIdsAny = documentSnapshot.get("chatIds")
                val chatIds = if (chatIdsAny is List<*>) {
                    chatIdsAny.filterIsInstance<String>()
                } else {
                    emptyList()
                }
                onUpdateChats(chatIds)
            }
        }.also { listenerRegistrations.add(it) }
    }

    fun setupChatListener(
        db: FirebaseFirestore,
        chatId: String,
        listenerRegistrations: MutableList<ListenerRegistration>,
        onError: (String) -> Unit,
        onUpdateChats: (List<ChatThread>) -> Unit
    ): ListenerRegistration {
        val chatRef = db.collection("chats").document(chatId)
        return chatRef.addSnapshotListener { chatDocumentSnapshot, chatError ->
            if (chatError != null) {
                Log.e("ChatHistoryScreen", "Failed to listen for chat updates.", chatError)
                onError("Failed to load chat data.")
                return@addSnapshotListener
            }
            if (chatDocumentSnapshot != null && chatDocumentSnapshot.exists()) {
                val chatThread = ChatThread(
                    friendId = chatDocumentSnapshot.getString("friendId") ?: "",
                    userId = chatDocumentSnapshot.getString("userId") ?: "",
                    chatId = chatDocumentSnapshot.id,
                    lastMessage = chatDocumentSnapshot.getString("lastMessage") ?: "",
                    lastMessageTime = chatDocumentSnapshot.getTimestamp("lastMessageTime") ?: Timestamp.now()
                )
                onUpdateChats(listOf(chatThread))
            }
        }.also { listenerRegistrations.add(it) }
    }

    @Suppress("UNCHECKED_CAST")
    // Function to load chat history
    fun loadChatHistory(
        userId: String,
        onSuccess: (List<ChatThread>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        firestore.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                val chatIds = document.get("chatIds") as? List<String> ?: emptyList()

                if (chatIds.isNotEmpty()) {
                    // Proceed with Firestore query only if chatIds is not empty
                    firestore.collection("chats").whereIn(FieldPath.documentId(), chatIds)
                        .get()
                        .addOnSuccessListener { snapshot ->
                            val chatThreads = snapshot.documents.map { doc ->
                                ChatThread(
                                    friendId = doc.getString("friendId") ?: "" ,
                                    userId = doc.getString("userId") ?: "",
                                    chatId = doc.id,
                                    lastMessage = doc.getString("lastMessage") ?: "",
                                    lastMessageTime = doc.getTimestamp("lastMessageTime") ?: Timestamp.now()
                                )
                            }
                            val sortedThreads = chatThreads.sortedByDescending { it.lastMessageTime }
                            _chats.value = sortedThreads
                            onSuccess(sortedThreads)
                        }
                        .addOnFailureListener(onFailure)
                } else {
                    // Handle case when there are no chatIds (i.e., no conversations)
                    onSuccess(emptyList())  // Return an empty list to indicate no chats
                }
            }
            .addOnFailureListener(onFailure)
    }
}
