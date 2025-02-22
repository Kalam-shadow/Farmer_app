package com.example.farmerapp.veggie

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.farmerapp.ui.theme.FarmerAppTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot

data class Order(
    val veggieName: String,
    val price: Double,
    val quantity: Int,
    val status: String
)

@Composable
fun MyOrdersScreen() {
    val firestore = FirebaseFirestore.getInstance()
    var orderList by remember { mutableStateOf<List<Order>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Load orders when the screen is first displayed
    LaunchedEffect(Unit) {
        loadOrders(firestore) { orders, error ->
            if (error != null) {
                errorMessage = error
            } else {
                orderList = orders ?: emptyList()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text(text = "My Orders", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(16.dp))

        errorMessage?.let { error ->
            Text(text = "Error: $error", color = MaterialTheme.colorScheme.error)
        }

        // Display orders or a message if the list is empty
        if (orderList.isEmpty() && errorMessage == null) {
            Text(text = "No orders placed yet.")
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(orderList) { order ->
                    OrderItem(order)
                }
            }
        }
    }
}

@Composable
fun OrderItem(order: Order) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Veggie: ${order.veggieName}")
            Text(text = "Price: ₹${order.price}")
            Text(text = "Quantity: ${order.quantity} kg")
            Text(text = "Status: ${order.status}")
        }
    }
}

private fun loadOrders(
    firestore: FirebaseFirestore,
    onResult: (List<Order>?, String?) -> Unit
) {
    val userId = FirebaseAuth.getInstance().currentUser?.uid
    if (userId == null) {
        Log.e("MyOrdersScreen", "User ID is null")
        onResult(null, "User not authenticated")
        return
    }

    firestore.collection("users").document(userId).collection("orders")
        .get()
        .addOnSuccessListener { result: QuerySnapshot ->
            val orders = result.documents.map { document ->
                Order(
                    veggieName = document.getString("veggieName") ?: "",
                    price = document.getDouble("price") ?: 0.0,
                    quantity = document.getDouble("quantity")?.toInt() ?: 0,
                    status = document.getString("status") ?: "Pending"
                )
            }
            onResult(orders, null)
        }
        .addOnFailureListener { exception ->
            Log.e("Firestore", "Error fetching orders", exception)
            onResult(null, exception.message)
        }
}

@Preview(showBackground = true)
@Composable
fun MyOrdersScreenPreview() {
    FarmerAppTheme {
        MyOrdersScreen()
    }
}
