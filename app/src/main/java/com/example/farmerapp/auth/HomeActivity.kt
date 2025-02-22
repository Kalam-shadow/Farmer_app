package com.example.farmerapp.auth

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coil.compose.rememberAsyncImagePainter
import com.example.farmerapp.R
import com.example.farmerapp.chats.ChatHistoryScreen
import com.example.farmerapp.profile.EditProfileActivity
import com.example.farmerapp.profile.ProfileActivity
import com.example.farmerapp.ui.theme.FarmerAppTheme
import com.example.farmerapp.veggie.MyOrdersScreen
import com.example.farmerapp.veggie.OrderVeggiesScreen
import com.example.farmerapp.veggie.UploadVeggiesScreen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuth.getInstance
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class HomeActivity : ComponentActivity() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var sharedPreferencesHelper: SharedPreferencesHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        firestore = FirebaseFirestore.getInstance()
        auth = getInstance()
        sharedPreferencesHelper = SharedPreferencesHelper(this)

        setContent {
            FarmerAppTheme {
                HomeScreen()
            }
        }
    }

    @Composable
    fun HomeScreen() {
        val context = LocalContext.current
        val currentId: String = getInstance().currentUser?.uid ?: ""
        val navController = rememberNavController()
        var job by remember { mutableStateOf("Loading...") }


        // Track the currently selected item
        var selectedItem by remember { mutableStateOf("Home") }

        Scaffold(
            topBar = { AppBar(context,selectedItem) },
            bottomBar = {
                Box {
                    LaunchedEffect(key1 = currentId) {
                        try {
                            val cachedprofession =  sharedPreferencesHelper.getProfession()
                            if(cachedprofession.isNullOrEmpty()){
                                val fetchedUserName = firestore.collection("users")
                                    .document(currentId)
                                    .get()
                                    .await()
                                    .getString("profession")

                                job = fetchedUserName ?: "You"
                                sharedPreferencesHelper.saveUser(profession = job)
                            }else{
                                    job = cachedprofession
                            }
                        } catch (e: Exception) {
                            Log.e("ChatScreen", "Error retrieving user's Profession", e)
                            job = "You"
                        }
                    }
                    BottomNavigationBar(navController,job) { newItem ->
                        selectedItem = newItem
                    }
                    if(job == "Farmer"){
                        FloatingActionButton(
                            onClick = {
                                   navController.navigate(NavRoute.UploadVeggies.route)
                            },
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .offset(y = (-15).dp),
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Add")
                        }
                    }

                }
            },
            content = { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    NavHost(navController = navController, startDestination = NavRoute.Home.route) {
                        composable(NavRoute.Home.route) { HomeTextScreen() }
                        composable(NavRoute.ViewVeggies.route) { ViewVeggiesScreen() }
                        composable(NavRoute.Messages.route) { ChatHistoryScreen(currentId) }
                        composable(NavRoute.MyOrders.route) { MyOrdersFrag() }
                        composable(NavRoute.UploadVeggies.route) { UploadVeggiesScreen(navController = navController) }

                        // Dynamic route for viewing specific veggies
                        composable(
                            route = "view_veggies/{veggieId}",
                            arguments = listOf(navArgument("veggieId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val veggieId = backStackEntry.arguments?.getString("veggieId")
                            ViewVeggiesScreen(veggieId)
                        }
                    }

                }
            }
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun AppBar(context: Context,selectedItem: String) {
        val currentId: String = getInstance().currentUser?.uid ?: ""
        TopAppBar(
            title = {
                var userName by remember { mutableStateOf("Loading...") }
                var imageUrl by remember { mutableStateOf("") }
                LaunchedEffect(key1 = currentId) {
                    try {
                        val cachedName = sharedPreferencesHelper.getName()
                        if (cachedName.isNullOrEmpty()) {
                            val fetchedUserName = firestore.collection("users")
                                .document(currentId)
                                .get()
                                .await()
                                .getString("name")
                            userName = fetchedUserName ?: "You"
                            sharedPreferencesHelper.saveUser(name = userName)
                        } else {
                            userName = cachedName
                        }
                    } catch (e: Exception) {
                        Log.e("AppBar", "Error fetching user name", e)
                        userName = "You"
                    }
                }
                LaunchedEffect(key1 = currentId) {
                    try {
                        val fetchpic = firestore.collection("users")
                            .document(currentId)
                            .get()
                            .await()
                            .getString("profileImageUrl")
                        imageUrl = fetchpic ?: ""
                    }catch(e: Exception) {
                        Log.e("AppBar", "Error fetching user name", e)
                        imageUrl = ""
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically

                ) {
                    // Profile Icon
                    val placeholderBitmap = BitmapFactory.decodeFile(sharedPreferencesHelper.getImagepath())
                    val painter = rememberAsyncImagePainter(
                        model = imageUrl,
                        placeholder = placeholderBitmap ?.let { BitmapPainter(it.asImageBitmap())} ?: painterResource(R.drawable.img), // Bitmap as placeholder
                        error = painterResource(R.drawable.art)
                    )

                    Image(
                        painter = painter,
                        contentDescription = "Profile Image",
                        modifier = Modifier
                            .size(40.dp) // Slightly smaller to create a padding effect inside the circle
                            .clip(CircleShape)
                            .clickable(onClick = {
                                val intent = Intent(context, ProfileActivity::class.java)
                                context.startActivity(intent)
                            }),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(12.dp))

                    Text(text = if (selectedItem == "Home")userName else selectedItem, style = MaterialTheme.typography.titleMedium)

                    Spacer(modifier = Modifier.weight(1f))

                    IconButton(onClick = {
                        val intent = Intent(context, EditProfileActivity::class.java)
                        context.startActivity(intent)
                    }) {
                        Icon(imageVector = Icons.Default.Edit,contentDescription = "Messages")
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary,
                titleContentColor = MaterialTheme.colorScheme.onPrimary
            )
        )
    }

    @Composable
    fun BottomNavigationBar(navController: NavHostController,
                            job: String,
                            //selectedItem: String,
                            onItemSelected: (String) -> Unit) {
        val items = listOf(
            BottomNavItem("Home", Icons.Filled.Home, "Home"),
            //BottomNavItem("My Veggies", Icons.Filled.ShoppingCart, "my_veggies"),
            BottomNavItem("Veggies", Icons.AutoMirrored.Filled.List, "Veggies"),
            BottomNavItem("Messages", Icons.Filled.Email, "Messages"),
            BottomNavItem("My Orders", Icons.Filled.ShoppingCart, "My Orders")
        )
        NavigationBar {
            items.forEachIndexed { index,item ->
                if (index == 2 && job == "Farmer") {
                    Spacer(modifier = Modifier.width(72.dp)) // Spacer for FAB
                }
                NavigationBarItem(
                    icon = { Icon(item.icon, contentDescription = item.label) },
                    label = { Text(item.label) },
                    selected = navController.currentBackStackEntryAsState().value?.destination?.route == item.route,
                    onClick = {
                        onItemSelected(item.route)
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.startDestinationId){saveState=true}
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    }

    sealed class NavRoute(val route: String) {
        data object Home : NavRoute("Home")
        data object ViewVeggies : NavRoute("Veggies")
        data object Messages : NavRoute("Messages")
        data object MyOrders : NavRoute("My Orders")
        data object UploadVeggies : NavRoute("UploadVeggies")
    }

    data class BottomNavItem(val label: String, val icon: ImageVector, val route: String)

    @Composable
    fun ViewVeggiesScreen() {
        OrderVeggiesScreen()
    }

    @Composable
    fun MyOrdersFrag() {
       MyOrdersScreen()
    }
    @Composable
    fun HomeTextScreen() {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally, // Center items horizontally
            verticalArrangement = Arrangement.Center // Center items vertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.art), // Replace with your logo resource ID
                contentDescription = "App Logo",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(150.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Welcome to Farmer App!", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(horizontal = 8.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Home Screen", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(horizontal = 8.dp))
        }
    }

    @Composable
    fun ViewVeggiesScreen(veggieId: String?) {
        Text(text = "Veggie ID: $veggieId")
    }

}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    FarmerAppTheme {
        HomeActivity().HomeScreen()
    }
}
