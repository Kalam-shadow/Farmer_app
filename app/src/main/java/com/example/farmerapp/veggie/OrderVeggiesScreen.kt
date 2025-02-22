package com.example.farmerapp.veggie

import android.content.Context
import android.content.Intent
import android.widget.Toast
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
import androidx.compose.ui.unit.dp
import com.example.farmerapp.chats.ChatActivity
import com.example.farmerapp.transaction.TransactionActivity
import com.google.firebase.auth.FirebaseAuth.getInstance
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun OrderVeggiesScreen() {
    val context = LocalContext.current // Get context from LocalContext
    FirebaseFirestore.getInstance()
    var veggieList by remember { mutableStateOf<List<Veggie>>(emptyList()) }

    LaunchedEffect(Unit) {
        loadAvailableVeggies(context) { veggies -> // Pass context here
            veggieList = veggies
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Available Veggies", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(16.dp))

        if (veggieList.isEmpty()) {
            Text(text = "No veggies available at the moment.")
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(veggieList) { veggie ->
                    VeggieItem(veggie = veggie, context = context)
                }
            }
        }
    }
}

@Composable
fun VeggieItem(veggie: Veggie, context: Context) {
    val currentId: String = getInstance().currentUser?.uid ?: ""
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Veggie: ${veggie.name}")
            Text(text = "Price: ₹${veggie.price}")
            Text(text = "Quantity: ${veggie.quantity} kg")
            Text(text = "Location: ${veggie.location}")

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = { orderVeggie(veggie, context) }) {
                Text("Order Now")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(onClick = {
                val intent = Intent(context, ChatActivity::class.java).apply {
                    putExtra("userId", currentId ) // Replace with actual userId
                    putExtra("friendId", veggie.farmerId)
                }
                context.startActivity(intent)
            }) {
                Text("Negotiate Price")
            }
        }
    }
}

// Modify the function to accept context
fun loadAvailableVeggies(context: Context, onResult: (List<Veggie>) -> Unit) {
    val firestore = FirebaseFirestore.getInstance()
    firestore.collection("veggies")
        .get()
        .addOnSuccessListener { result ->
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
        .addOnFailureListener {
            Toast.makeText(context, "Failed to fetch veggies", Toast.LENGTH_SHORT).show()
            onResult(emptyList())  // Returning empty list in case of failure
        }
}

fun orderVeggie(veggie: Veggie, context: Context) {
    val intent = Intent(context, TransactionActivity::class.java)
    intent.putExtra("veggieName", veggie.name)
    intent.putExtra("price", veggie.price)
    intent.putExtra("quantity", veggie.quantity)
    intent.putExtra("farmerId", veggie.farmerId)
    context.startActivity(intent)
}
