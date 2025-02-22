@file:Suppress("DEPRECATION")

package com.example.farmerapp.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModelProvider
import com.example.farmerapp.R
import com.example.farmerapp.profile.EditProfileActivity
import com.example.farmerapp.profile.LoadingScreen
import com.example.farmerapp.ui.theme.FarmerAppTheme
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class AuthActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var authViewModel: AuthViewModel
    private lateinit var sharedPreferencesHelper: SharedPreferencesHelper
    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPreferencesHelper = SharedPreferencesHelper(this)
        auth = FirebaseAuth.getInstance()

        // Use the factory to create the ViewModel with SharedPreferencesHelper dependency
        val factory = AuthViewModelFactory(sharedPreferencesHelper)
        authViewModel = ViewModelProvider(this, factory)[AuthViewModel::class.java]

        // Configure Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Register the Activity Result Launcher
        val googleSignInLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                account?.idToken?.let { authViewModel.firebaseAuthWithGoogle(it) }
            } catch (e: ApiException) {
                Toast.makeText(this, "Google sign-in failed", Toast.LENGTH_SHORT).show()
            }
        }

        authViewModel.checkUserLoggedIn()

        // Load saved credentials if available
        val savedEmail = sharedPreferencesHelper.getEmail()
        val savedPassword = sharedPreferencesHelper.getPassword()
        if (!savedEmail.isNullOrEmpty() && !savedPassword.isNullOrEmpty()) {
            authViewModel.login(savedEmail, savedPassword, onSuccess = { navigateToHome() }) {
                Toast.makeText(this, "Auto-login failed: $it", Toast.LENGTH_LONG).show()
            }
        }

        // Observe authentication state to handle navigation or display messages
        authViewModel.authState.observe(this) { state ->
            when (state) {
                is AuthViewModel.AuthState.Loading -> {
                    // Optionally, show a loading indicator here
                }

                is AuthViewModel.AuthState.Success -> {
                    // Access firebaseAuth from the ViewModel instance
                    authViewModel.firebaseAuth.currentUser?.email?.let { email ->
                        sharedPreferencesHelper.saveUser(email = email)
                    }
                    if (!hasNavigatedToHome) navigateToHome()
                }

                is AuthViewModel.AuthState.Error -> {
                    // Show an error message if authentication fails
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                }

                else -> { /* Idle or other states do nothing */ }
            }
        }

        setContent {
            FarmerAppTheme {
                Box {
                    Surface(color = MaterialTheme.colorScheme.background){
                        AuthScreenUI(googleSignInLauncher)
                    }
                    LoadingScreen()
                }
            }
        }
    }

    @Composable
    private fun AuthScreenUI(googleSignInLauncher: ActivityResultLauncher<Intent>) {
        AuthScreen(
            onSuccess = { email, password ->
                authViewModel.login(
                    email.toString(), password.toString(),
                    onSuccess = { navigateToHome() },
                    onError = { errorMessage ->
                        Toast.makeText(this@AuthActivity, errorMessage, Toast.LENGTH_SHORT).show()
                    }
                )
            },
            onRegister = { email, password ->
                authViewModel.register(
                    email.toString(), password.toString(),
                    onError = { errorMessage ->
                        Toast.makeText(this@AuthActivity, errorMessage, Toast.LENGTH_SHORT).show()
                    }
                )
            },
            onGoogleSignIn = {
                signInWithGoogle(googleSignInLauncher)
                //authViewModel.initializeGoogleSignIn()
                // Handle Google sign-in result callback in firebaseAuthWithGoogle function
            }
        )
    }

    private fun signInWithGoogle(googleSignInLauncher: ActivityResultLauncher<Intent>) {
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    private var hasNavigatedToHome = false
    private val isLoading = mutableStateOf(false)
    private fun navigateToHome() {
        if (hasNavigatedToHome) return
        hasNavigatedToHome = true
        isLoading.value=true

        val user = auth.currentUser
        isProfileIncomplete(user) { isIncomplete ->
            isLoading.value=false
            if (isIncomplete) {
                startActivity(Intent(this, EditProfileActivity::class.java))
            } else {
                startActivity(Intent(this, HomeActivity::class.java))
            }
            finish()
        }
    }

    private fun isProfileIncomplete(user: FirebaseUser?, onComplete: (Boolean) -> Unit) {
        if (user == null) {
            onComplete(true) // User is null, so profile is incomplete
            return
        }

        val cachedName = sharedPreferencesHelper.getName()
        val  cachedPhone = sharedPreferencesHelper.getPhone()
        if(cachedName.isNullOrEmpty() && !cachedPhone.isNullOrEmpty()){
            onComplete(false)
            return
        }

        CoroutineScope(Dispatchers.IO).launch{
            try{
                val firestore = FirebaseFirestore.getInstance()
                val userRef = firestore.collection("users").document(user.uid)
                val document = userRef.get().await()

                withContext(Dispatchers.Main){
                    if(document.exists()){
                        val name = document.getString("name")
                        val phone = document.getString("phone")
                        if(!name.isNullOrEmpty() && !phone.isNullOrEmpty()){
                            sharedPreferencesHelper.saveUser(name = name, phone =  phone,)
                            onComplete(false)
                        }else{
                            onComplete(true)
                        }
                    }else{
                        onComplete(true)
                    }
                }
            }catch (e: Exception){
                withContext(Dispatchers.Main){
                    onComplete(true)
                }
            }
        }
    }
}
