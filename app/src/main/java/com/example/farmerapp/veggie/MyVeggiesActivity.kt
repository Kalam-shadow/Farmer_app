package com.example.farmerapp.veggie

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.farmerapp.ui.theme.FarmerAppTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot


data class Veggie(
    val name: String,
    val price: Double,
    val quantity: Int,
    val location: String,
    val farmerId: String
)

class MyVeggiesActivity : ComponentActivity() {

    private lateinit var firestore: FirebaseFirestore
    private var veggieListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        firestore = FirebaseFirestore.getInstance()

        setContent {
            FarmerAppTheme {
                MyVeggiesScreen()
            }
        }
    }

    @Composable
    fun MyVeggiesScreen() {
        var veggieList by remember { mutableStateOf<List<Veggie>>(emptyList()) }
        LocalContext.current

        // Load veggies when the screen is first displayed
        LaunchedEffect(Unit) {
            loadVeggies { veggies ->
                veggieList = veggies
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Text(text = "My Veggies", style = MaterialTheme.typography.headlineMedium)

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
//                    val intent = Intent(context, UploadVeggiesActivity::class.java)
//                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Upload New Veggie")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Display veggies or a message if the list is empty
            if (veggieList.isEmpty()) {
                Text(text = "No veggies uploaded yet.")
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(veggieList) { veggie ->
                        VeggieItem(veggie)
                    }
                }
            }
        }
    }

    @Composable
    fun VeggieItem(veggie: Veggie) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Veggie: ${veggie.name}")
                Text(text = "Price: ₹${veggie.price}")
                Text(text = "Quantity: ${veggie.quantity} kg")
                Text(text = "Location: ${veggie.location}")
                Text(text = "Farmer ID: ${veggie.farmerId}")
            }
        }
    }

    private fun loadVeggies(onResult: (List<Veggie>) -> Unit) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Log.e("MyVeggiesActivity", "com.example.farmerapp.com.example.farmerapp.User ID is null")
            return
        }

        firestore.collection("users").document(userId).collection("veggies")
            .get()
            .addOnSuccessListener { result: QuerySnapshot ->
                val veggies = result.documents.map { document ->
                    Veggie(
                        name = document.getString("name") ?: "",
                        price = document.getDouble("price") ?: 0.0,
                        quantity = document.getDouble("quantity")?.toInt() ?: 0,
                        location = document.getString("location") ?: "",
                        farmerId = document.getString("farmerId") ?: ""
                    )
                }
                onResult(veggies)
            }
            .addOnFailureListener { exception ->
                Log.e("Firestore", "Error fetching veggies", exception)
                onResult(emptyList())
            }
    }


    override fun onDestroy() {
        super.onDestroy()
        veggieListener?.remove() // Clean up Firestore listener
    }
}
@Preview(showBackground = true)
@Composable
fun MyVeggiesScreenPreview() {
    FarmerAppTheme {
        MyVeggiesActivity().MyVeggiesScreen()
    }
}
