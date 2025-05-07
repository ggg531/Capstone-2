package com.example.foodtap.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import kotlinx.coroutines.delay

@Composable
fun SigninScreen(navController: NavController, viewModel: SigninViewModel = viewModel()) {
    LaunchedEffect(Unit) {
        delay(500)
        viewModel.speak("식품 톡톡에 오신 것을 환영합니다!")
        delay(3000)
        navController.navigate("init") {
            popUpTo("signin") { inclusive = true }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "식품 톡톡에 오신 것을\n환영합니다!",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            lineHeight = 40.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "식품 톡톡에 오신 것을 환영합니다!" }
        )
        Spacer(modifier = Modifier.height(26.dp))
        // 아이콘
    }
}