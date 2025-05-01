package com.example.foodtap.feature.init
import com.example.foodtap.ui.theme.Main

import android.Manifest
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import kotlinx.coroutines.flow.collectLatest

// 예시 메인 색상
val Main = Color(0xFF6200EE)

@Composable
fun InitScreen(
    navController: NavController,
    viewModel: InitViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // --- 권한 요청 런처 ---
    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted: Boolean ->
            Log.d("InitScreen", "Permission result: $isGranted")
            viewModel.onPermissionResult(isGranted) // ViewModel에 결과 전달
            if (!isGranted) {
                Toast.makeText(context, "마이크 권한이 거부되었습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    )

    // --- 네비게이션 처리 ---
    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collectLatest {
            Log.d("InitScreen", "Navigation event received, navigating...")
            // TODO: "next_screen_route" 를 실제 다음 화면 라우트로 변경!
            val nextRoute = "my"
            navController.navigate(nextRoute) {
                // Optional: Configure navigation options
                // popUpTo(navController.graph.startDestinationId) { inclusive = true }
            }
        }
    }

    // --- 권한 요청 상태 감지 및 실행 ---
    LaunchedEffect(uiState.flowState) {
        if (uiState.flowState == InitFlowState.REQUESTING_PERMISSION) {
            Log.d("InitScreen", "State is REQUESTING_PERMISSION, launching permission launcher.")
            recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // --- UI 레이아웃 ---
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Main)
            .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
            .background(Color.White)
            // ▼▼▼ 탭/더블 탭 감지 ▼▼▼
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { offset -> // offset 정보도 받을 수 있음
                        Log.d("InitScreen", "onTap detected at $offset")
                        viewModel.onSingleTap()
                    },
                    onDoubleTap = { offset ->
                        Log.d("InitScreen", "onDoubleTap detected at $offset")
                        viewModel.onDoubleTap()
                    }
                )
            }
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center // 컨텐츠 수직 중앙 정렬
    ) {

        // --- 메인 컨텐츠 영역 (상태 기반 UI) ---
        Box(
            modifier = Modifier
                .weight(1f) // Column 내에서 남은 공간 차지
                .fillMaxWidth(),
            contentAlignment = Alignment.Center // Box 내부 요소 중앙 정렬
        ) {
            // 상태에 따른 UI 분기
            when (uiState.flowState) {
                InitFlowState.INITIALIZING -> InitStateUI("초기화 중...", showProgress = true)

                InitFlowState.SPEAKING_WELCOME,
                InitFlowState.SPEAKING_RESULT -> InitStateUI(uiState.displayText, uiState.interactionHint, showProgress = true)

                InitFlowState.REQUESTING_PERMISSION -> InitStateUI(uiState.displayText, uiState.interactionHint, showProgress = true)

                InitFlowState.LISTENING -> InitStateUI(uiState.displayText, showProgress = true)

                InitFlowState.PROCESSING_RESULT -> InitStateUI("결과 처리 중...", showProgress = true)

                InitFlowState.WAITING_USER_INTERACTION,
                InitFlowState.IDLE,
                InitFlowState.ERROR -> InteractiveStateUI(uiState)
            }
        }
    }
}

/**
 * 초기화, 진행 중 상태 표시를 위한 간단한 Composable Helper
 */
@Composable
private fun InitStateUI(
    displayText: String,
    interactionHint: String = "",
    showProgress: Boolean = false
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (showProgress) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
        }
        Text(
            text = displayText,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            textAlign = TextAlign.Center
        )
        if (interactionHint.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = interactionHint,
                fontSize = 14.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * 사용자 입력 대기, IDLE, 에러 상태 표시를 위한 Composable Helper
 */
@Composable
private fun InteractiveStateUI(uiState: InitUiState) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(16.dp) // 패딩 추가
    ) {
        // 메인 텍스트 (환영 메시지 또는 결과)
        Text(
            text = uiState.displayText,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )

        // 인식된 텍스트가 있다면 추가로 표시 (선택적)
        uiState.recognizedText?.let { recognized ->
            if (uiState.displayText != "결과: \"$recognized\"") { // displayText 와 중복 표시 방지
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "(인식된 내용: \"$recognized\")",
                    fontSize = 14.sp,
                    color = Color.DarkGray,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 에러 메시지 표시
        uiState.errorMessage?.let { error ->
            Text(
                text = "오류: $error",
                color = MaterialTheme.colorScheme.error, // Use theme color for error
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // 상호작용 안내 문구
        if (uiState.interactionHint.isNotEmpty()) {
            Text(
                text = uiState.interactionHint,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                color = Color.Gray
            )
        }
    }
}