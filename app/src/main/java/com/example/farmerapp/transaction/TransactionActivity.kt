package com.example.farmerapp.transaction

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.example.farmerapp.R
import com.example.farmerapp.ui.theme.FarmerAppTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.razorpay.Checkout
import com.razorpay.PaymentResultListener
import kotlinx.coroutines.launch
import org.json.JSONObject

class TransactionActivity : ComponentActivity(), PaymentResultListener {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var veggieName: String = ""
    private var price: Double = 0.0
    private var quantity: Int = 0
    private var farmerId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FarmerAppTheme {
                TransactionScreen()
            }
        }

        // Initialize Firebase Auth and Firestore
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Get veggie details from intent
        veggieName = intent.getStringExtra("veggieName") ?: ""
        price = intent.getDoubleExtra("price", 0.0)
        quantity = intent.getIntExtra("quantity", 0)
        farmerId = intent.getStringExtra("farmerId") ?: ""

        // Start the Razorpay checkout process
        startPayment()
    }

    @Composable
    fun TransactionScreen() {
        LocalContext.current
        var isLoading by remember { mutableStateOf(false) }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(text = "Veggie Name: $veggieName")
            Text(text = "Price: $price")
            Text(text = "Quantity: $quantity")
            Spacer(modifier = Modifier.height(16.dp))
            if (isLoading) {
                CircularProgressIndicator()
            } else {
                Button(
                    onClick = {
                        isLoading = true
                        startPayment()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Confirm Order")
                }
            }
        }
    }

    private fun startPayment() {
        val checkout = Checkout()
        checkout.setKeyID("rzp_test_oJHPgraJu6r7Jc") // Replace with actual Razorpay API key

        // Calculate the amount in the smallest currency unit (paise for INR)
        val amount = (price * 100).toInt()

        try {
            val options = JSONObject().apply {
                put("name", "Farmer App")
                put("description", "Payment for $veggieName")
                put("image", R.drawable.art)
                put("amount", amount) // Ensure amount is > 0 and in smallest currency unit
                put("currency", "INR") // Correct currency
                put("prefill", JSONObject().apply {
                    put("email", auth.currentUser?.email ?: "test@example.com") // Fallback to a test email if null
                    put("contact", auth.currentUser?.phoneNumber ?: "9999999999") // Fallback to a dummy phone number if null
                })
            }
            Log.d("RazorpayOptions", "Options: $options")
            checkout.open(this, options)
        } catch (e: Exception) {
            Toast.makeText(this, "Error in payment: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
            Log.d("RazorpayError", "Error: ${e.message}")
        }
    }

    override fun onPaymentSuccess(razorpayPaymentID: String) {
        val userId = auth.currentUser?.uid ?: return
        val orderData = mapOf(
            "veggieName" to veggieName,
            "price" to price,
            "quantity" to quantity,
            "farmerId" to farmerId,
            "status" to "Completed",
            "paymentId" to razorpayPaymentID
        )

        lifecycleScope.launch {
            try {
                firestore.collection("users").document(userId).collection("orders")
                    .add(orderData)
                Toast.makeText(this@TransactionActivity, "Order placed successfully!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@TransactionActivity, "Failed to save order!", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }
        finish()
    }

    override fun onPaymentError(code: Int, response: String?) {
        Toast.makeText(this, "Payment failed: $response", Toast.LENGTH_SHORT).show()
        Log.d("razerpayerror", "Error: $response")
        finish()
    }
}

@Preview(showBackground = true)
@Composable
fun TransactionScreenPreview() {
    TransactionActivity().TransactionScreen()
}
