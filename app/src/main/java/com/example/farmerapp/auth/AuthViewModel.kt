package com.example.farmerapp.auth

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider

class AuthViewModel(private val sharedPreferencesHelper: SharedPreferencesHelper) : ViewModel() {

    val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()

    sealed class AuthState {
        data object Idle : AuthState()
        data object Loading : AuthState()
        data class Success(val user: FirebaseUser?) : AuthState()
        data class Error(val message: String) : AuthState()
    }

    private val _authState = MutableLiveData<AuthState>(AuthState.Idle)
    val authState: LiveData<AuthState> = _authState

    fun login(email: String, password: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        _authState.value = AuthState.Loading
        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    sharedPreferencesHelper.saveUser(email = email, password = password)
                    onSuccess()
                    _authState.value = AuthState.Success(firebaseAuth.currentUser)
                } else {
                    onError(task.exception?.message ?: "Login failed")
                    _authState.value = AuthState.Error(task.exception?.message ?: "Unknown error")
                }
            }
    }

    fun register(email: String, password: String, onError: (String) -> Unit) {
        _authState.value = AuthState.Loading
        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = firebaseAuth.currentUser
                    _authState.value = AuthState.Success(user)

                    user?.let {
                        if (!it.isEmailVerified){
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

                } else {
                    onError(task.exception?.message ?: "Registration failed")
                    _authState.value = AuthState.Error(task.exception?.message ?: "Unknown error")
                }
            }
    }


    fun checkUserLoggedIn() {
        val currentUser = firebaseAuth.currentUser
        if (currentUser != null) {
            _authState.value = AuthState.Success(currentUser)
        } else {
            _authState.value = AuthState.Idle
        }
    }

    fun firebaseAuthWithGoogle(idToken: String) {
        val credential: AuthCredential = GoogleAuthProvider.getCredential(idToken, null)
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = firebaseAuth.currentUser
                    _authState.value = AuthState.Success(user)

                    user?.let {
                        if (!it.isEmailVerified){
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
                } else {
                    _authState.value = AuthState.Error(task.exception?.message ?: "Google sign-in failed")
                }
            }
    }
}
