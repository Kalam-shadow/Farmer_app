package com.example.farmerapp.veggie

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun UploadVeggiesScreen(navController: NavController) {
    // State variables to hold the input values
    var veggieName by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var locationAddress by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) } // Track loading state
    val context = LocalContext.current

    // Composables for the screen UI
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Input fields for veggie details
        TextField(
            value = veggieName,
            onValueChange = { veggieName = it },
            label = { Text("Veggie Name") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = price,
            onValueChange = { price = it },
            label = { Text("Price") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = quantity,
            onValueChange = { quantity = it },
            label = { Text("Quantity") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Manual input for location (address or region)
        TextField(
            value = locationAddress,
            onValueChange = { locationAddress = it },
            label = { Text("Location (Address or Region)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Button to upload veggie details
        Button(
            onClick = {
                // Input validation
                val priceValue = price.toDoubleOrNull()
                val quantityValue = quantity.toIntOrNull()

                if (veggieName.isNotEmpty() && priceValue != null && quantityValue != null && locationAddress.isNotEmpty()) {
                    isLoading = true // Show loading
                    saveVeggieDetails(veggieName, priceValue, quantityValue, locationAddress, context) {
                        isLoading = false // Hide loading after completion
                        navController.popBackStack() // Navigate back to the previous screen after success
                    }
                } else {
                    Toast.makeText(context, "Please fill out all fields correctly", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isLoading) {
                Text("Uploading...") // Change button text while loading
            } else {
                Text("Upload Veggie")
            }
        }
    }
}

// Function to save veggie details to Firestore
private fun saveVeggieDetails(
    veggieName: String,
    price: Double,
    quantity: Int,
    location: String,
    context: Context,
    onComplete: () -> Unit
) {
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
    val farmerId = FirebaseAuth.getInstance().currentUser?.uid ?: return

    val veggieData = hashMapOf(
        "name" to veggieName,
        "price" to price,
        "quantity" to quantity,
        "location" to location,
        "farmerId" to farmerId
    )

    val db = FirebaseFirestore.getInstance()
    val globalVeggiesRef = db.collection("veggies").document()
    val userVeggiesRef = db.collection("users").document(userId).collection("veggies").document()

    db.runBatch { batch ->
        batch.set(globalVeggiesRef, veggieData)
        batch.set(userVeggiesRef, veggieData)
    }.addOnSuccessListener {
        Toast.makeText(context, "Veggie details uploaded successfully", Toast.LENGTH_SHORT).show()
        onComplete() // Call the callback after successful upload
    }.addOnFailureListener {
        Toast.makeText(context, "Failed to upload veggie details", Toast.LENGTH_SHORT).show()
        onComplete() // Hide loading even on failure
    }
}


