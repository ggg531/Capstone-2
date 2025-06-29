package com.example.foodtap.feature.user

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.foodtap.feature.init.ApiStatus
import com.example.foodtap.ui.theme.Main
import com.example.foodtap.ui.theme.Safe
import com.example.foodtap.ui.theme.Unsafe
import com.example.foodtap.util.FileManager
import kotlinx.coroutines.delay

@Composable
fun SetExpScreen(navController: NavController, viewModel: MyViewModel = viewModel()) {
    val context = LocalContext.current.applicationContext
    val userData = remember { FileManager.loadUserData(context) }

    var count by remember { mutableStateOf(userData?.expi_date?.toIntOrNull() ?: 5) }
    var readyToNavigate by remember { mutableStateOf(false) }
    val isTtsDone by viewModel.isTtsDone.collectAsState()

    LaunchedEffect(Unit) {
        delay(500)
        viewModel.speak("소비 기한을 변경하고, 중간에 있는 버튼을 클릭하세요.")
    }

    LaunchedEffect(isTtsDone) {
        if (isTtsDone && readyToNavigate) {
            navController.navigate("my") {
                popUpTo("setexp") { inclusive = true }
            }
            readyToNavigate = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.5f)
                .background(Safe)
                .clickable {
                    count++
                    viewModel.speak("${count}일")
                }
                .semantics { contentDescription = "소비 기한 증가" }
                .align(Alignment.TopCenter),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "+",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.5f)
                .background(Unsafe)
                .clickable {
                    if (count > 0)
                        count--
                        viewModel.speak("${count}일")
                }
                .semantics { contentDescription = "기한 감소" }
                .align(Alignment.BottomCenter),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "-",
                fontSize = 64.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
            )
        }

        Button(
            onClick = {
                viewModel.confirmResult(count) { success ->
                    if (success) {
                        viewModel.stopSpeaking()
                        viewModel.speakWithCallback("소비기한이 ${count}일로 변경되었습니다.")
                        readyToNavigate = true
                    } else {
                        viewModel.speak("소비기한 정보를 다시 등록하세요.")
                    }
                }
            },
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Main),
            modifier = Modifier
                .size(width = 330.dp, height = 80.dp)
                .align(Alignment.Center)
        ) {
            Text(
                text = "${count} 일",
                fontSize = 32.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                modifier = Modifier.semantics {
                    contentDescription = "최소 소비 기한은 ${count}일 입니다."
                }
            )
        }
    }
}