package com.example.foodtap.feature.ocr

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController

@Composable
fun OcrScreen(
    navController: NavController,
    viewModel: OcrViewModel = viewModel()
) {
    val passedBitmap = navController.previousBackStackEntry
        ?.savedStateHandle
        ?.get<Bitmap>("bitmap")

    val ocrText by viewModel.ocrResult.collectAsState()

    LaunchedEffect(passedBitmap) {
        passedBitmap?.let {
            viewModel.startOcr(it)
        }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        passedBitmap?.let {
            Image(bitmap = it.asImageBitmap(), contentDescription = null)
            Spacer(modifier = Modifier.height(16.dp))
        }
        Text(text = ocrText)
    }
}