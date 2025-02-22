package com.example.farmerapp.profile

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import coil.compose.rememberAsyncImagePainter
import com.example.farmerapp.R
import com.example.farmerapp.auth.HomeActivity
import com.example.farmerapp.auth.SharedPreferencesHelper
import com.example.farmerapp.chats.ChatHistoryViewModel
import com.example.farmerapp.ui.theme.FarmerAppTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuth.getInstance
import com.google.firebase.firestore.FirebaseFirestore
import java.io.FileNotFoundException

class EditProfileActivity : ComponentActivity() {
    private var imageUri: Uri? = null
    private val firebaseAuth: FirebaseAuth = getInstance()
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var profileViewModel: ProfileViewModel
    private lateinit var sharedPreferencesHelper: SharedPreferencesHelper

    // Use Activity Result API for picking images
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            imageUri = uri
            val bitmap = uriToBitmap(it)
            if (bitmap != null) {
                authenticateAndUploadImage(uri,bitmap)
            }
        }
    }

    // Convert Uri to Bitmap
    private fun uriToBitmap(uri: Uri): Bitmap? {
        return try {
            val inputStream = contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream)
        }
        catch (e: FileNotFoundException) {
            e.printStackTrace()
            null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FirebaseFirestore.getInstance().clearPersistence().addOnCompleteListener {
            FirebaseFirestore.getInstance().enableNetwork()
        }

        firestore = FirebaseFirestore.getInstance()
        auth = getInstance()
        sharedPreferencesHelper = SharedPreferencesHelper(this)

        val factory = ProfileViewModelFactory(sharedPreferencesHelper)
        profileViewModel = ViewModelProvider(this, factory)[ProfileViewModel::class.java]

        ViewModelProvider(this)[ChatHistoryViewModel::class.java]

        val currentUser = auth.currentUser

        if (currentUser == null || !currentUser.isEmailVerified) {
            Toast.makeText(this, "Please log in and verify your email to access this page.", Toast.LENGTH_LONG).show()
            firebaseAuth.currentUser?.let {
                if(!it.isEmailVerified){
                    it.sendEmailVerification()
                        .addOnCompleteListener { emailTask ->
                            if (emailTask.isSuccessful) {
                                Log.d("registration", "Email sent successfully")
                            }else{
                                Log.d("registration", "Email sent failed")
                            }
                        }
                }
            }
        }

        if (currentUser != null) {
            profileViewModel.loadUserProfile(uid = currentUser.uid)
        }

        setContent {
            FarmerAppTheme {
                EditProfileScreen(
                    profileViewModel = profileViewModel,
                    onSaveProfile = { userProfile->
                        profileViewModel.saveUserProfile(userProfile) { success ->
                            handleSaveProfileResult(success)
                        }
                    },
                    onCancel = { finish() },
                    onProfileImageClick = { pickImageFromGallery() },
                    sharedPreferencesHelper = sharedPreferencesHelper
                )
            }
        }
    }

    private fun pickImageFromGallery() {
        pickImageLauncher.launch("image/*")
    }

    private fun handleSaveProfileResult(success: Boolean) {
        if (success) {
            Toast.makeText(this, "Profile saved successfully!", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, HomeActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
            finish()
        } else {
            Toast.makeText(this, "Failed to save profile.", Toast.LENGTH_SHORT).show()
        }
    }
    private fun authenticateAndUploadImage(uri: Uri,bitmap: Bitmap) {
        profileViewModel.uploadImageToFirebase(uri,bitmap,this,
            onSuccess = { downloadUrl ->
                profileViewModel.updateProfileImage(downloadUrl)
                profileViewModel.saveImageUrlToFirestore(downloadUrl,
                    onSuccess = {
                        Toast.makeText(this, "Image uploaded and saved successfully.", Toast.LENGTH_SHORT).show()
                                },
                    onFailure = { exception ->
                        Log.e("Firestore", "Failed to save image URL", exception)
                        Toast.makeText(this, "Failed to save image URL.", Toast.LENGTH_SHORT).show()
                    }
                )
                        },
            onFailure = { exception ->
                Log.e("Upload", "Failed to upload image", exception)
                Toast.makeText(this, "Failed to upload image.", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    profileViewModel: ProfileViewModel,
    onSaveProfile: (UserProfile) -> Unit,
    onCancel: () -> Unit,
    onProfileImageClick: () -> Unit,
    sharedPreferencesHelper: SharedPreferencesHelper
) {
    val userState by profileViewModel.userState.collectAsState()
    val profileImageUrl by profileViewModel.profileImageUrl.observeAsState()

    // Track individual field states and initialize only after userState is loaded
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var shortBio by remember { mutableStateOf("") }
    var aadharNumber by remember { mutableStateOf("") }
    var profession by remember { mutableStateOf("") }
    var birthday by remember { mutableStateOf("") }
    var instagramAccount by remember { mutableStateOf("") }
    var isInitialized by remember { mutableStateOf(false) }
    val professionOptions = listOf("Customer", "Farmer")
    var chatIds by remember { mutableStateOf(emptyList<String>()) }
    var isDropdownExpanded by remember { mutableStateOf(false) }


    // Populate fields with user data only when first loaded
    LaunchedEffect(userState) {
        userState?.let { user ->
            if (!isInitialized) {
                name = user.name.toString()
                phone = user.phone.toString()
                shortBio = user.shortBio.toString()
                aadharNumber = user.aadharNumber.toString()
                profession = user.profession.toString()
                birthday = user.birthday.toString()
                instagramAccount = user.instagramAccount.toString()
                chatIds = user.chatIds!!
                isInitialized = true
            }
        }
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Edit Profile", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.height(20.dp))

        // Improved Profile Image UI
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
                .clickable { onProfileImageClick() },
            contentAlignment = Alignment.Center
        ) {

            val imageUrl by profileViewModel.profileImageUrl.observeAsState()
            val placeholderBitmap = BitmapFactory.decodeFile(sharedPreferencesHelper.getImagepath())
            val painter = rememberAsyncImagePainter(
                model = imageUrl,
                placeholder = placeholderBitmap ?.let { BitmapPainter(it.asImageBitmap())} ?: painterResource(R.drawable.img), // Bitmap as placeholder
                error = painterResource(R.drawable.art)
            )

            Image(
                painter = painter,
                contentDescription = "Profile Image",
                modifier = Modifier
                    .size(90.dp) // Slightly smaller to create a padding effect inside the circle
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        }


        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Dropdown for profession selection
        ExposedDropdownMenuBox(
            expanded = isDropdownExpanded,
            onExpandedChange = { isDropdownExpanded = !isDropdownExpanded }
        ) {
            OutlinedTextField(
                value = profession,
                onValueChange = {},
                label = { Text("Profession") },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true), // Updated usage
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(isDropdownExpanded) },
                readOnly = true
            )
            ExposedDropdownMenu(
                expanded = isDropdownExpanded,
                onDismissRequest = { isDropdownExpanded = false }
            ) {
                professionOptions.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            profession = option
                            isDropdownExpanded = false
                        },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface) // Set background color
                    )
                }
            }
        }


        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = phone,
            onValueChange = { if (it.all { char -> char.isDigit() }) phone = it },
            label = { Text("Phone") },
            modifier = Modifier.fillMaxWidth(),
            isError = phone.length != 10
        )
        if (phone.length != 10) {
        Text(
            text = "Phone number should be 10 digits",
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.labelSmall
        )
    }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = shortBio,
            onValueChange = { if (it.length <= 150) shortBio = it },
            label = { Text("Short Bio (Max 150 characters)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = aadharNumber,
            onValueChange = { if (it.all { char -> char.isDigit() }) aadharNumber = it },
            label = { Text("Aadhar Number") },
            modifier = Modifier.fillMaxWidth(),
            isError = aadharNumber.length != 12
        )
        if (aadharNumber.length != 12) {
            Text(
                text = "Aadhar number should be 12 digits",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelSmall
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = birthday,
            onValueChange = { birthday = it },
            label = { Text("Birthday") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = instagramAccount,
            onValueChange = { instagramAccount = it },
            label = { Text("Instagram Account") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(onClick = onCancel) {
                Text("Cancel")
            }
            Button(onClick = {
                val email = getInstance().currentUser?.email ?: ""
                val profileImage = profileImageUrl ?: userState?.profileImageUrl ?: "" // Use existing profileImageUrl if null
                val updatedUser = UserProfile(
                    uid = getInstance().currentUser?.uid ?: "",
                    name = name,
                    phone = phone,
                    shortBio = shortBio,
                    aadharNumber = aadharNumber,
                    profession = profession,
                    birthday = birthday,
                    instagramAccount = instagramAccount,
                    email = email,
                    profileImageUrl = profileImage,
                    chatIds = chatIds
                )
                onSaveProfile(updatedUser)
            }) {
                Text("Save")
            }

        }
    }
}
