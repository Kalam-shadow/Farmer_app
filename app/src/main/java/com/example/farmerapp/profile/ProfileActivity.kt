package com.example.farmerapp.profile

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import coil.compose.rememberAsyncImagePainter
import com.example.farmerapp.R
import com.example.farmerapp.auth.AuthActivity
import com.example.farmerapp.auth.SharedPreferencesHelper
import com.example.farmerapp.ui.theme.FarmerAppTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuth.getInstance
import com.google.firebase.firestore.FirebaseFirestore

private val isLoading = mutableStateOf(false)


class ProfileActivity : ComponentActivity() {
    private lateinit var profileViewModel: ProfileViewModel
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var sharedPreferencesHelper: SharedPreferencesHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        firestore = FirebaseFirestore.getInstance()
        auth = getInstance()
        sharedPreferencesHelper = SharedPreferencesHelper(this)

        FirebaseFirestore.getInstance().clearPersistence().addOnCompleteListener {
            FirebaseFirestore.getInstance().enableNetwork()
        }

        val factory = ProfileViewModelFactory(sharedPreferencesHelper)
        profileViewModel = ViewModelProvider(this, factory)[ProfileViewModel::class.java]

        setContent {
            FarmerAppTheme {
                ProfileScreen(profileViewModel = profileViewModel)
            }
        }
    }
}

@Composable
fun ProfileScreen(profileViewModel: ProfileViewModel) {
    val userState by profileViewModel.userState.collectAsState(initial = null)
    val loadingState by profileViewModel.loadingState.collectAsState()

    if (loadingState) {
        LoadingScreen()
    } else {
        userState?.let { userProfile ->
            ProfileContent(userProfile = userProfile, profileViewModel = profileViewModel)
        }
    }
}

@Composable
fun ProfileContent(userProfile: UserProfile, profileViewModel: ProfileViewModel) {
    val context = LocalContext.current

    Box(modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ProfileHeader(userProfile,profileViewModel)

            Spacer(modifier = Modifier.height(16.dp))

            ProfileBadgeSection(userProfile)

            Spacer(modifier = Modifier.height(16.dp))

            ProfileCompletionBar(profileViewModel = profileViewModel)

            Spacer(modifier = Modifier.height(16.dp))

            ProfileInfoList(userProfile)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            FloatingActionButton(onClick = {
                val intent = Intent(context, EditProfileActivity::class.java)
                context.startActivity(intent)
            }) {
                Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Profile")
            }
            FloatingActionButton(
                onClick = {
                    val sharedPreferencesHelper = SharedPreferencesHelper(context)
                    sharedPreferencesHelper.clearCredentials()
                    getInstance().signOut() // Ensure you have the correct instance

// Start AuthActivity after logout with flags to clear the task stack
                    val intent = Intent(context, AuthActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    if (context is Activity) {
                        context.finish()
                    }

                },
                containerColor = MaterialTheme.colorScheme.error) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout", tint = MaterialTheme.colorScheme.onError)
            }
            FloatingActionButton(onClick = {
                // Implement profile sharing functionality
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, "Check out my profile!")
                }
                context.startActivity(Intent.createChooser(shareIntent, "Share Profile"))
            }) {
                Icon(imageVector = Icons.Default.Share, contentDescription = "Share Profile")
            }
        }
    }
}

@Composable
fun ProfileHeader(userProfile: UserProfile,profileViewModel: ProfileViewModel) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            UserProfileImage(profileViewModel,SharedPreferencesHelper(LocalContext.current))
        }


        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = userProfile.name ?: "Name not available",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = userProfile.shortBio ?: "Bio not available",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
fun ProfileBadgeSection(user: UserProfile) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        if (user.isTopSeller) {
            BadgeIcon(Icons.Default.Star, "Top Seller")
        }
        if (user.isVerifiedBuyer) {
            BadgeIcon(Icons.Default.Check, "Verified Buyer")
        }
    }
}

@Composable
fun UserProfileImage(viewModel: ProfileViewModel,sharedPreferencesHelper: SharedPreferencesHelper) {
    val imageUrl by viewModel.profileImageUrl.observeAsState()
    val placeholderBitmap = BitmapFactory.decodeFile(sharedPreferencesHelper.getImagepath())
    val painter = rememberAsyncImagePainter(
        model = imageUrl,
        placeholder = placeholderBitmap ?.let { BitmapPainter(it.asImageBitmap())} ?: painterResource(R.drawable.img), // Bitmap as placeholder
        error = painterResource(R.drawable.art)
    )
    Image(
        painter = painter,
        contentDescription = "Profile Image",
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .size(128.dp)
            .clip(CircleShape)
    )
}

@Composable
fun BadgeIcon(icon: ImageVector, description: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(MaterialTheme.colorScheme.secondary, CircleShape)
            .padding(8.dp)
    ) {
        Icon(imageVector = icon, contentDescription = description, tint = MaterialTheme.colorScheme.onSecondary)
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = description, color = MaterialTheme.colorScheme.onSecondary)
    }
}

@Composable
fun ProfileCompletionBar(profileViewModel: ProfileViewModel) {
    val progress: Float = profileViewModel.getProfileCompletionPercentage()
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Profile Completion", color = MaterialTheme.colorScheme.onBackground)
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
        )
    }
}

@Composable
fun ProfileInfoList(userProfile: UserProfile) {
    Column {
        ProfileInfoItem(icon = Icons.Default.Person, text = userProfile.name ?: "Not available")
        ProfileInfoItem(icon = Icons.Default.Favorite, text = userProfile.birthday ?: "Birthday not set")
        ProfileInfoItem(icon = Icons.Default.Call, text = userProfile.phone ?: "Phone not set")
        ProfileInfoItem(icon = Icons.Default.AccountBox, text = userProfile.instagramAccount ?: "Instagram not set")
        ProfileInfoItem(icon = Icons.Default.Email, text = userProfile.email)
        ProfileInfoItem(icon = Icons.Default.CheckCircle, text = "Password")
    }
}

@Composable
fun ProfileInfoItem(icon: ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurface)
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun LoadingScreen() {
    if (isLoading.value){
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}













