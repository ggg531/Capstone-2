package com.example.foodtap.feature.camera

import android.content.Context
import android.graphics.Bitmap
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel

class CameraViewModel : ViewModel() {
    private var cameraManager: CameraManager? = null

    fun initCamera(context: Context, lifecycleOwner: LifecycleOwner): PreviewView {
        if (cameraManager == null) {
            cameraManager = CameraManager(context, lifecycleOwner)
        }
        return cameraManager!!.startCamera()
    }

    fun takePhoto(onBitmapCaptured: (Bitmap?) -> Unit) {
        cameraManager?.takePicture(onBitmapCaptured)
    }
}
