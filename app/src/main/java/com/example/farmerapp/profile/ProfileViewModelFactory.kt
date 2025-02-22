package com.example.farmerapp.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.farmerapp.auth.SharedPreferencesHelper

class ProfileViewModelFactory(
    private val sharedPreferencesHelper: SharedPreferencesHelper
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProfileViewModel(sharedPreferencesHelper) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
