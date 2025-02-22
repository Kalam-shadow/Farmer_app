package com.example.farmerapp.auth

import android.content.Context
import android.content.SharedPreferences
import com.example.farmerapp.models.User


class SharedPreferencesHelper(context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    // Save the entire user object
    fun saveUser(user: User) {
        sharedPreferences.edit().apply {
            putString("name", user.name)
            putString("email", user.email)
            putString("password", user.password)
            putString("profession", user.profession)
            putString("phone", user.phone)
            putString("imagepath", user.imagepath)
            apply()
        }
    }

    // Overloaded methods to update individual elements without losing other data
    fun saveUser(
        name: String? = null,
        email: String? = null,
        password: String? = null,
        profession: String? = null,
        phone: String? = null,
        imagepath: String? = null
    ) {
        val editor = sharedPreferences.edit()
        name?.let { editor.putString("name", it) }
        email?.let { editor.putString("email", it) }
        password?.let { editor.putString("password", it) }
        profession?.let { editor.putString("profession", it) }
        phone?.let { editor.putString("phone", it) }
        imagepath?.let { editor.putString("imagepath", it) }
        editor.apply()
    }

    // Retrieve user data
    fun getUser(): User {
        val name = sharedPreferences.getString("name", "") ?: ""
        val email = sharedPreferences.getString("email", null)
        val password = sharedPreferences.getString("password", null)
        val profession = sharedPreferences.getString("profession", null)
        val phone = sharedPreferences.getString("phone", null)
        val imagepath = sharedPreferences.getString("imagepath", null)
        return User(name, email, password, profession, phone)
    }

    // Retrieve specific user data elements
    fun getName(): String? = sharedPreferences.getString("name", null)
    fun getEmail(): String? = sharedPreferences.getString("email", null)
    fun getPassword(): String? = sharedPreferences.getString("password", null)
    fun getProfession(): String? = sharedPreferences.getString("profession", null)
    fun getPhone(): String? = sharedPreferences.getString("phone", null)
    fun getImagepath(): String? = sharedPreferences.getString("imagepath", null)

    // Clear user data
    fun clearCredentials() {
        sharedPreferences.edit().clear().apply()
    }
}
