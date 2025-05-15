package com.example.foodtap

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.foodtap.feature.auth.SigninScreen
import com.example.foodtap.feature.camera.CameraPermission
import com.example.foodtap.feature.init.InitScreen
import com.example.foodtap.feature.user.MyScreen
import com.example.foodtap.feature.user.SetCriteriaScreen
import com.example.foodtap.feature.user.SetExpScreen
import com.example.foodtap.feature.user.SetUiScreen
import com.example.foodtap.ui.BottomBar
import com.example.foodtap.ui.TopBar

@Composable
fun MainApp() {
    val navController = rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    Scaffold(
        topBar = {
            if (currentRoute in listOf("my", "setexp")) {
                TopBar(navController)
            }
        },
        bottomBar = {
            if (currentRoute in listOf("camera", "my", "setcri", "setui")) {
                BottomBar(navController)
            }
        }
    ) {  innerPadding->
        NavHost(
            navController = navController,
            startDestination = "signin",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("signin") { SigninScreen(navController) }
            composable("init") { InitScreen(navController) }
            composable("camera") { CameraPermission(navController) }
            composable("my") { MyScreen(navController) }
            composable("setexp") { SetExpScreen(navController) }
            //composable("setcri") { SetCriteriaScreen(navController) }
            //composable("setui") { SetUiScreen(navController) }
        }
    }
}