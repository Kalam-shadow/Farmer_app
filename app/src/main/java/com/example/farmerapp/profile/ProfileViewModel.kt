package com.example.farmerapp.profile

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.farmerapp.auth.SharedPreferencesHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


data class UserProfile(
    val name: String? = null,
    val email: String,
    val profession: String? = null,
    val phone: String? = null,
    val aadharNumber: String? = null,
    val shortBio: String? = null,
    val profileImageUrl: String? = null,
    val birthday: String? = null,  // Birthday added
    val instagramAccount: String? = null,  // Instagram account added
    val uid: String,
    val isTopSeller: Boolean = false,  // Top seller badge
    val isVerifiedBuyer: Boolean = false,// Verified buyer badge
    val chatIds: List<String>? = emptyList()
    //val completionPercentage: Float = 0f  // Completion percentage
){
    // No-argument constructor required by Firestore
    constructor() : this(
        name = null,
        email = "",
        profession = null,
        phone = null,
        aadharNumber = null,
        shortBio = null,
        profileImageUrl = null,
        birthday = null,
        instagramAccount = null,
        uid = "",
        isTopSeller = false,
        isVerifiedBuyer = false,
        chatIds = listOf()
    )
}
class ProfileViewModel(private var sharedPreferencesHelper: SharedPreferencesHelper) : ViewModel() {
    private val _userState = MutableStateFlow<UserProfile?>(null)
    val userState: StateFlow<UserProfile?> = _userState

    private val _loadingState = MutableStateFlow(false)
    val loadingState: StateFlow<Boolean> = _loadingState

    private val _profileImageUrl = MutableLiveData<String?>()
    val profileImageUrl: MutableLiveData<String?> get() = _profileImageUrl

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val userId = auth.currentUser?.uid

    init {
        userId?.let { loadUserProfile(it) } ?: Log.e("ProfileViewModel", "User is not authenticated")
        profilePicLoader()
    }

    // Function to fetch user profile from Firestore
    private fun profilePicLoader() {
        val userId = auth.currentUser?.uid ?: return
        val userRef = firestore.collection("users").document(userId)
        userRef.get()
            .addOnSuccessListener { documentSnapshot ->
                val profileImageUrl = documentSnapshot.getString("profileImageUrl")
                _profileImageUrl.postValue(profileImageUrl)
            }
            .addOnFailureListener { exception ->
                Log.e("UserProfileViewModel", "Failed to fetch user profile", exception)
            }
    }

    // Function to load user profile from Firestore
    fun loadUserProfile(uid: String) {
        viewModelScope.launch {
            _loadingState.value = true
            firestore.collection("users").document(uid)
                .get()
                .addOnSuccessListener { document ->
                    _userState.value = document?.toObject(UserProfile::class.java)
                }
                .addOnFailureListener { exception ->
                    Log.e("ProfileViewModel", "Error loading profile: ${exception.message}")
                    _userState.value = null
                }
                .addOnCompleteListener {
                    _loadingState.value = false
                }
        }
    }

    // Function to save user profile to Firestore
    fun saveUserProfile(userProfile: UserProfile, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            _loadingState.value = true

            if (userProfile.email.isEmpty() || userProfile.name.isNullOrEmpty()) {
                Log.e("ProfileViewModel", "Validation failed: Email and Name are required")
                onComplete(false)
                _loadingState.value = false
                return@launch
            }

            userId?.let {
                firestore.collection("users").document(it).set(userProfile)
                    .addOnSuccessListener {
                        _userState.value = userProfile

                        sharedPreferencesHelper.saveUser(
                            name = userProfile.name,
                            email = userProfile.email,
                            profession = userProfile.profession,
                            phone = userProfile.phone
                        )
                        Log.d("ProfileViewModel", "Profile saved successfully")
                        onComplete(true)
                    }
                    .addOnFailureListener { exception ->
                        Log.e("ProfileViewModel", "Failed to save profile: ${exception.message}")
                        onComplete(false)
                    }
                    .addOnCompleteListener {
                        _loadingState.value = false
                    }
            }
        }
    }

    // Function to validate Uri
    private fun validateUri(uri: Uri, context: Context): Boolean {
        return try {
            context.contentResolver.openInputStream(uri)?.use {
                true
            } ?: false
        } catch (e: IOException) {
            Log.e("ImageValidation", "Invalid image Uri", e)
            false
        }
    }

    // Function to upload image to Firebase Storage
    fun uploadImageToFirebase(uri: Uri,bitmap: Bitmap, context: Context, onSuccess: (String) -> Unit, onFailure: (Exception) -> Unit) {
        if (!validateUri(uri, context)) {
            onFailure(Exception("Invalid image Uri."))
            return
        }

        val storageReference = FirebaseStorage.getInstance().reference
        val userId = auth.currentUser?.uid ?: return
        val profileImagesRef = storageReference.child("Images/profile_images/$userId.jpg")

        profileImagesRef.putFile(uri)
            .addOnSuccessListener {
                profileImagesRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                    Log.d("FirebaseStorage", "Download URL: $downloadUrl")
                    onSuccess(downloadUrl.toString())
                    saveImage(context, bitmap,userId)
                    _profileImageUrl.value = downloadUrl.toString()
                }
            }
            .addOnFailureListener(onFailure)
    }

    private fun saveImage(context: Context, bitmap: Bitmap, imageName: String) {
        val directory = context.getDir("images", Context.MODE_PRIVATE)
        val imageFile = File(directory, "$imageName.png")

        try {
            FileOutputStream(imageFile).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
            }
            val imagepath = imageFile.absolutePath
            sharedPreferencesHelper.saveUser(imagepath = imagepath)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun saveImageUrlToFirestore(downloadUrl: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.e("SaveImageUrl", "User ID is null")
            return
        }
        val userRef = firestore.collection("users").document(userId)
        if (downloadUrl.isEmpty()) {
            Log.d("Firestore", "Image URL is empty")
            return
        }
        userRef.update("profileImageUrl", downloadUrl)
            .addOnSuccessListener {
                Log.d("Firestore", "Image URL saved to Firestore: $downloadUrl")
                onSuccess()
            }
            .addOnFailureListener { exception ->
                Log.e("Firestore", "Failed to save image URL", exception)
                onFailure(exception)
            }
    }

    // Update profile image URL in state
    fun updateProfileImage(downloadUrl: String) {
        _userState.value = _userState.value?.copy(profileImageUrl = downloadUrl)
        Log.d("UpdateProfileImage", "Updated profile image URL: ${_userState.value?.profileImageUrl}")
    }



    // Calculate profile completion percentage
    fun getProfileCompletionPercentage(): Float {
        val user = _userState.value ?: return 0f

        val totalFields = 8 // Adjust this to the correct number of optional fields
        val completedFields = listOf(
            user.name, user.profession, user.phone, user.aadharNumber,
            user.shortBio, user.profileImageUrl, user.birthday, user.instagramAccount
        ).count { !it.isNullOrEmpty() }

        return completedFields / totalFields.toFloat()
    }
}
