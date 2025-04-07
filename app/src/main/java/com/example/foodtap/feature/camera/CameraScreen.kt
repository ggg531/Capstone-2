package com.example.foodtap.feature.camera

import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.foodtap.ui.theme.Main

@Composable
fun CameraScreen(
    navController: NavController,
    viewModel: CameraViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val previewView = remember {
        viewModel.initCamera(context, lifecycleOwner)
    }

    CameraContent(
        previewView = previewView,
        onCapture = {
            viewModel.takePhoto { bitmap ->
                bitmap?.let {
                    navController.currentBackStackEntry
                        ?.savedStateHandle
                        ?.set("bitmap", it)
                    navController.navigate("ocr")
                }
            }
        }
    )
}

@Composable
fun CameraContent(
    previewView: PreviewView,
    onCapture: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally

            ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(width = 330.dp, height = 72.dp)
                    .border(2.dp, Color.White, RoundedCornerShape(16.dp))
                    .background(Main, RoundedCornerShape(16.dp))
            ) {
                Text(
                    text = "식품 정보를 촬영하세요.",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
            }
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            IconButton(
                onClick = onCapture,
                modifier = Modifier
                    .size(96.dp)
                    .background(Color.Black.copy(alpha = 0.5f), MaterialTheme.shapes.large)
                    .semantics { contentDescription = "촬영 버튼" }
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoCamera,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(56.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "촬영",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}
