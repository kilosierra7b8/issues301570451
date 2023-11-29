package com.example.myapplication

import android.Manifest
import android.content.ContentResolver
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.Matrix
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.TotalCaptureResult
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.myapplication.MainActivity.Companion.TAG
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApplicationTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CameraTest()
                }
            }
        }
    }

    companion object {
        const val TAG = "MainActivity"
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraTest(modifier: Modifier = Modifier) {
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    val context = LocalContext.current as ComponentActivity

    var isManual by remember { mutableStateOf(false) }
    var sensitivity by remember { mutableStateOf<Int?>(null) }
    var postRawSensitivityBoost by remember { mutableStateOf<Int?>(null) }
    var exposureTime by remember { mutableStateOf<Long?>(null) }
    var frameDuration by remember { mutableStateOf<Long?>(null) }

    var bitmap by remember { mutableStateOf<Bitmap?>(null) }

    Column {
        if (cameraPermissionState.status.isGranted) {
            Button(onClick = {
                isManual = false
                startCamera(context = context, isManual = false) { iso: Int?, boost: Int?, exTime: Long?, duration: Long?, preview: Bitmap ->
                    sensitivity = iso
                    postRawSensitivityBoost = boost
                    exposureTime = exTime
                    frameDuration = duration
                    bitmap = preview
                }
            }, modifier = Modifier.padding(8.dp)) {
                Text("Auto")
            }
            Button(onClick = {
                isManual = true
                startCamera(
                    context = context,
                    isManual = true,
                    paramSensitivity = sensitivity,
                    paramPostRawSensitivityBoost = postRawSensitivityBoost,
                    paramExposureTime = exposureTime,
                    paramFrameDuration = frameDuration) { iso: Int?, boost: Int?, exTime: Long?, duration: Long?, preview: Bitmap ->
                    sensitivity = iso
                    postRawSensitivityBoost = boost
                    exposureTime = exTime
                    frameDuration = duration
                    bitmap = preview
                }
            }, modifier = Modifier.padding(8.dp)) {
                Text("Manual")
            }
            Button(onClick = {
                stopCamera(context = context)
            }, modifier = Modifier.padding(8.dp)) {
                Text("Stop")
            }

            Button(onClick = {
                saveImage(context = context)
            }, modifier = Modifier.padding(8.dp)) {
                Text("Save")
            }


            Text( text = "isManual = $isManual, sensitivity = $sensitivity, postBoost = $postRawSensitivityBoost, exposureTime = $exposureTime",
                fontWeight = FontWeight.Bold,
                modifier = Modifier
            )

            if (bitmap != null) {
                Image(
                    bitmap = bitmap!!.asImageBitmap(),
                    contentDescription = null,
                    modifier.fillMaxSize()
                )
            }
        }
        else {
            SideEffect {
                cameraPermissionState.launchPermissionRequest()
            }
            Button(onClick = {
                cameraPermissionState.launchPermissionRequest()
            }, modifier = Modifier.padding(8.dp)) {
                Text(text = "Launch permission request")
            }
        }
    }
}

@androidx.annotation.OptIn(ExperimentalCamera2Interop::class)
private fun configCamera(
    isManual: Boolean,
    builder: ImageAnalysis.Builder,
    paramSensitivity: Int? = null,
    paramPostRawSensitivityBoost: Int? = null,
    paramExposureTime: Long? = null,
    paramFrameDuration: Long? = null,
    onCaptured: (CaptureResult) -> Unit): ImageAnalysis.Builder {
    val extender = Camera2Interop.Extender(builder)

    extender
        .setCaptureRequestOption(
            CaptureRequest.CONTROL_MODE,
            CameraMetadata.CONTROL_MODE_AUTO,
        )

    if (isManual) {
        extender
            .setCaptureRequestOption(
                CaptureRequest.CONTROL_AE_MODE,
                CameraMetadata.CONTROL_AE_MODE_OFF
            )
            .setCaptureRequestOption(CaptureRequest.SENSOR_SENSITIVITY, paramSensitivity!!)
            .setCaptureRequestOption(CaptureRequest.CONTROL_POST_RAW_SENSITIVITY_BOOST, paramPostRawSensitivityBoost!!)
            .setCaptureRequestOption(
                CaptureRequest.SENSOR_EXPOSURE_TIME,
                paramExposureTime!!
            )
            .setCaptureRequestOption(CaptureRequest.SENSOR_FRAME_DURATION, paramFrameDuration!!)
    }
    else {
        extender
            .setCaptureRequestOption(
                CaptureRequest.CONTROL_AE_MODE,
                CameraMetadata.CONTROL_AE_MODE_ON
            )
    }

    extender.setSessionCaptureCallback(object : CameraCaptureSession.CaptureCallback() {
        override fun onCaptureCompleted(
            session: CameraCaptureSession,
            request: CaptureRequest,
            result: TotalCaptureResult
        ) {
            onCaptured(result)
        }
    })

    return builder
}

fun stopCamera(context: ComponentActivity) {
    ProcessCameraProvider.getInstance(context).get().unbindAll()
}

fun startCamera(
    context: ComponentActivity,
    isManual: Boolean,
    paramSensitivity: Int? = null,
    paramPostRawSensitivityBoost: Int? = null,
    paramExposureTime: Long? = null,
    paramFrameDuration: Long? = null,
    onCaptured: (Int?, Int?, Long?, Long?, Bitmap) -> Unit) {

    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

    val cameraExecutor = Executors.newSingleThreadExecutor()

    cameraProviderFuture.addListener({
        val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

        var sensorSensitivity: Int? = null
        var postRawSensitivityBoost: Int? = null
        var sensorExposureTime: Long? = null
        var frameDuration: Long? = null

        val imageAnalyzerBuilder = configCamera(
            isManual = isManual,
            builder = ImageAnalysis.Builder(),
            paramSensitivity = paramSensitivity,
            paramPostRawSensitivityBoost = paramPostRawSensitivityBoost,
            paramExposureTime = paramExposureTime,
            paramFrameDuration = paramFrameDuration,
        ) {
            sensorSensitivity = it.get(CaptureResult.SENSOR_SENSITIVITY)
            postRawSensitivityBoost = it.get(CaptureResult.CONTROL_POST_RAW_SENSITIVITY_BOOST)
            sensorExposureTime = it.get(CaptureResult.SENSOR_EXPOSURE_TIME)
            frameDuration = it.get(CaptureResult.SENSOR_FRAME_DURATION)
        }

        val imageAnalyzer = imageAnalyzerBuilder
            .build()
            .also {
                it.setAnalyzer(cameraExecutor
                ) { imageProxy ->
                    val bitmap = imageProxy.toBitmap()
                    val matrix = Matrix().apply { postRotate(90f) }

                    val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

                    onCaptured(sensorSensitivity, postRawSensitivityBoost, sensorExposureTime, frameDuration, rotated)

                    imageProxy.close()
                }
            }

        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        try {
            cameraProvider.unbindAll()

            cameraProvider.bindToLifecycle(
                context, cameraSelector, imageAnalyzer,
            )
        } catch(e: Exception) {
            Log.e(TAG, "Use case binding failed", e)
        }
    }, ContextCompat.getMainExecutor(context))
}

private fun saveImage(context: ComponentActivity) {
    val ss = context.window.decorView.rootView
    ss.isDrawingCacheEnabled = true
    val bitmap = Bitmap.createBitmap(ss.drawingCache)
    ss.isDrawingCacheEnabled = false

    val stream = ByteArrayOutputStream()

    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)

    val path = Environment.DIRECTORY_DOWNLOADS + File.separator + "TestCamera"

    val resolver = context.contentResolver

    saveFile(
        file = stream.toByteArray(),
        values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, System.currentTimeMillis().toString() + ".png")
            put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
            put(MediaStore.MediaColumns.RELATIVE_PATH, path)
        },
        resolver = resolver
    )
}
private fun saveFile(file: ByteArray, values: ContentValues, resolver: ContentResolver) {
    try {
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: throw IOException("Failed to create new MediaStore record.")

        resolver.openOutputStream(uri)?.use {
            it.write(file)
        } ?: throw IOException("Failed to open output stream.")
    } catch (e: IOException) {
        throw e
    }
}
