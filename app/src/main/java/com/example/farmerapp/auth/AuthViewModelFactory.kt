package com.example.farmerapp.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

@Suppress("UNCHECKED_CAST")
class AuthViewModelFactory(
    private val sharedPreferencesHelper: SharedPreferencesHelper
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            return AuthViewModel(sharedPreferencesHelper) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
