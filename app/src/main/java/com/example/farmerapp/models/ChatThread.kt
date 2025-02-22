// File: models/ChatThread.kt
package com.example.farmerapp.models

import com.google.firebase.Timestamp

data class ChatThread(
    val friendId: String,
    val userId : String,
    val lastMessage: String,
    val lastMessageTime: Timestamp,
    val chatId: String
){
    constructor() : this("", "",  "", Timestamp.now(), "")
}


data class User(
    var name: String = "",
    var email: String? = null,
    var password: String? = null,
    var profession: String? = null,
    var phone: String? = null,
    var imagepath: String? = null
)
