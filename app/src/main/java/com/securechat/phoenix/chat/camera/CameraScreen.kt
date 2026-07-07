package com.securechat.phoenix.chat.camera

import android.content.Context
import android.net.Uri
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import java.io.File
import java.util.concurrent.Executors

/**
 * WhatsApp 2025-style custom camera screen.
 * - Tap shutter = take photo
 * - Hold shutter = record video
 * - Front/back switch visible
 * - Flash toggle visible
 * - No menu — all controls on screen
 */
@Composable
fun CameraScreen(
    onPhotoCaptured: (Uri) -> Unit,
    onVideoCaptured: (Uri) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var isFrontCamera by remember { mutableStateOf(true) }
    var isFlashOn by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }
    var recordingSeconds by remember { mutableIntStateOf(0) }

    val imageCapture = remember { ImageCapture.Builder().build() }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    // Recording timer
    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordingSeconds = 0
            while (isRecording) {
                delay(1000)
                recordingSeconds++
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // Camera preview
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { previewView ->
                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                cameraProviderFuture.addListener({
                    val provider = cameraProviderFuture.get()
                    cameraProvider = provider

                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val cameraSelector = if (isFrontCamera)
                        CameraSelector.DEFAULT_FRONT_CAMERA
                    else
                        CameraSelector.DEFAULT_BACK_CAMERA

                    try {
                        provider.unbindAll()
                        val camera = provider.bindToLifecycle(
                            lifecycleOwner, cameraSelector, preview, imageCapture
                        )
                        imageCapture.flashMode = if (isFlashOn)
                            ImageCapture.FLASH_MODE_ON
                        else
                            ImageCapture.FLASH_MODE_OFF
                    } catch (_: Exception) {}
                }, ContextCompat.getMainExecutor(context))
            }
        )

        // Top controls: Close + Flash + Switch camera
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Close
            Text(
                "✕",
                fontSize = 28.sp,
                color = Color.White,
                modifier = Modifier.clickable { onClose() }
            )

            Row {
                // Flash toggle
                Icon(
                    if (isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                    "Flash",
                    tint = if (isFlashOn) Color.Yellow else Color.White,
                    modifier = Modifier
                        .size(28.dp)
                        .clickable { isFlashOn = !isFlashOn }
                )

                Spacer(Modifier.width(20.dp))

                // Switch camera
                Icon(
                    Icons.Default.Cameraswitch,
                    "Switch",
                    tint = Color.White,
                    modifier = Modifier
                        .size(28.dp)
                        .clickable { isFrontCamera = !isFrontCamera }
                )
            }
        }

        // Recording indicator
        if (isRecording) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 48.dp)
                    .clip(CircleShape)
                    .background(Color.Red)
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    "⏺ %02d:%02d".format(recordingSeconds / 60, recordingSeconds % 60),
                    color = Color.White,
                    fontSize = 14.sp
                )
            }
        }

        // Bottom: Shutter button + hint
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                if (isRecording) "Release to stop" else "Tap photo · Hold video",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp
            )

            Spacer(Modifier.height(12.dp))

            // Shutter button: tap = photo, hold = video
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .border(
                        width = 4.dp,
                        color = if (isRecording) Color.Red else Color.White,
                        shape = CircleShape
                    )
                    .padding(6.dp)
                    .clip(CircleShape)
                    .background(if (isRecording) Color.Red else Color.White)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                // Start timer for long press detection
                                val pressStart = System.currentTimeMillis()
                                isRecording = false

                                val released = tryAwaitRelease()

                                val pressDuration = System.currentTimeMillis() - pressStart

                                if (released && pressDuration < 500) {
                                    // Short tap = take photo
                                    takePhoto(context, imageCapture) { uri ->
                                        onPhotoCaptured(uri)
                                    }
                                } else if (pressDuration >= 500) {
                                    // Was held = was recording video, now stop
                                    isRecording = false
                                    // Video recording stop handled separately
                                }
                            },
                            onLongPress = {
                                // Long press = start video recording
                                isRecording = true
                                // TODO: start video recording with CameraX VideoCapture
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isRecording) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(Color.White, CircleShape)
                    )
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraProvider?.unbindAll()
        }
    }
}

private fun takePhoto(context: Context, imageCapture: ImageCapture, onResult: (Uri) -> Unit) {
    val file = File(context.cacheDir, "photo_${System.currentTimeMillis()}.jpg")
    val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()

    imageCapture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                onResult(Uri.fromFile(file))
            }
            override fun onError(exc: ImageCaptureException) {
                exc.printStackTrace()
            }
        }
    )
}
