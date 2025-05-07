package com.example.foodtap.feature.user

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.foodtap.ui.theme.Main
import kotlinx.coroutines.delay

@Composable
fun MyScreen(navController: NavController, viewModel: MyViewModel = viewModel()) {
    LaunchedEffect(Unit) {
        delay(500)
        viewModel.speak("마이 페이지입니다. 화면 상단에는 구매 식품에 대한 소비 기한 정보가 있고, 화면 하단에는 구매 기준을 변경하는 버튼이 있습니다.")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Main)
            .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
            .background(Color.White)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
        ) {
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "식품 톡톡 님", // 닉네임
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            modifier = Modifier.semantics { contentDescription = "마이 페이지" }
        )
        Text(text = "사용자 설정 정보 표기...") // 소비기한 관리

        Spacer(modifier = Modifier.weight(1f))

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 30.dp),
            thickness = 1.dp,
            color = Color.LightGray
        )
        Column(
            modifier = Modifier.padding(bottom = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally
            ) {
            Button(
                onClick = {
                    viewModel.stopSpeaking()
                    navController.navigate("setcri")
                },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Main),
                modifier = Modifier.size(width = 360.dp, height = 72.dp)
            ) {
                Text(
                    text = "구매 기준 변경",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    modifier = Modifier.semantics { contentDescription = "구매 기준 변경 버튼" }
                )
            }

            Spacer(modifier = Modifier.height(30.dp))

            Button(
                onClick = {
                    viewModel.stopSpeaking()
                    navController.navigate("setui")
                },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Main),
                modifier = Modifier.size(width = 360.dp, height = 72.dp)
            ) {
                Text(
                    text = "화면 설정 변경",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    modifier = Modifier.semantics { contentDescription = "화면 설정 변경 버튼" }
                )
            }
        }
    }
}