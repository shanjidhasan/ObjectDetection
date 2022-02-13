package com.example.jetcomposepractice

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.launch

import androidx.compose.runtime.rememberCoroutineScope

import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.common.model.LocalModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.ObjectDetector
import com.google.mlkit.vision.objects.custom.CustomObjectDetectorOptions
import kotlinx.coroutines.launch

public lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
public lateinit var objectDetector: ObjectDetector

class MainActivity : ComponentActivity() {

    private lateinit var objectDetector: ObjectDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
//        cameraProviderFuture.addListener({
//            val cameraProvider = cameraProviderFuture.get()
//            bindPreview(cameraProvider = cameraProvider)
//        }, ContextCompat.getMainExecutor(this))
//
//        val localModel = LocalModel.Builder()
//            .setAbsoluteFilePath("object_detection.tflite")
//            .build()
//        val customObjectDetectorOptions = CustomObjectDetectorOptions.Builder(localModel)
//            .setDetectorMode(CustomObjectDetectorOptions.STREAM_MODE)
//            .enableClassification()
//            .setClassificationConfidenceThreshold(0.5f)
//            .setMaxPerObjectLabelCount(3)
//            .build()
//        objectDetector = ObjectDetection.getClient(customObjectDetectorOptions)
        setContent {
            CameraPreview(Modifier.fillMaxSize());
        }
    }
    @SuppressLint("UnsafeOptInUsageError")
    private fun bindPreview(cameraProvider: ProcessCameraProvider) {
        val preview = Preview.Builder().build()
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()
        val previewView = PreviewView(this).apply {
            this.scaleType = scaleType
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        preview.setSurfaceProvider(previewView.surfaceProvider)

        val imageAnalysis = ImageAnalysis.Builder()
            .setTargetResolution(Size(1280, 720))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this)) { imageProxy ->
            val rotationDegrees = imageProxy.imageInfo.rotationDegrees

            val image = imageProxy.image

            if (image != null) {
                val processImage = InputImage.fromMediaImage(image, rotationDegrees)
                objectDetector
                    .process(processImage)
                    .addOnSuccessListener { objects ->
                        for (i in objects) {
                            Log.v("MainActivity", "Object - ${i.labels.firstOrNull()?.text ?: "Undefined"}")
                        }
                        imageProxy.close()
                    }.addOnFailureListener {
                        Log.v("MainActivity", "Error - ${it.message}")
                        imageProxy.close()
                    }
            }
        }
        cameraProvider.bindToLifecycle(this as LifecycleOwner, cameraSelector, imageAnalysis, preview)

    }

}


@SuppressLint("UnsafeOptInUsageError")
@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    scaleType: PreviewView.ScaleType = PreviewView.ScaleType.FILL_CENTER,
    cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
) {
    val coroutineScope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    AndroidView(
        modifier = modifier,
        factory = { context ->
            val previewView = PreviewView(context).apply {
                this.scaleType = scaleType
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }

            val localModel = LocalModel.Builder()
                .setAbsoluteFilePath("object_detection.tflite")
                .build()
            val customObjectDetectorOptions = CustomObjectDetectorOptions.Builder(localModel)
                .setDetectorMode(CustomObjectDetectorOptions.STREAM_MODE)
                .enableClassification()
                .setClassificationConfidenceThreshold(0.5f)
                .setMaxPerObjectLabelCount(3)
                .build()
            objectDetector = ObjectDetection.getClient(customObjectDetectorOptions)

            val imageAnalysis = ImageAnalysis.Builder()
                .setTargetResolution(Size(1280, 720))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(context)) { imageProxy ->
                val rotationDegrees = imageProxy.imageInfo.rotationDegrees

                val image = imageProxy.image

                if (image != null) {
                    val processImage = InputImage.fromMediaImage(image, rotationDegrees)
                    objectDetector
                        .process(processImage)
                        .addOnSuccessListener { objects ->
                            for (i in objects) {
                                Log.v("MainActivity", "1111Object - ${i.labels.firstOrNull()?.text ?: "Undefined"}")
                            }
                            imageProxy.close()
                        }.addOnFailureListener {
                            Log.v("MainActivity", "Error - ${it.message}")
                            imageProxy.close()
                        }
                }
            }

            // Preview
            val previewUseCase = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            coroutineScope.launch {
                val cameraProvider = cameraProviderFuture.get()
                try {
                    // Must unbind the use-cases before rebinding them.
                    cameraProvider.unbindAll()

                    cameraProvider.bindToLifecycle(
                        lifecycleOwner, cameraSelector, previewUseCase
                    )
                } catch (ex: Exception) {
                    Log.e("CameraPreview", "Use case binding failed", ex)
                }
            }

            previewView
        }
    )
}

//@Composable
//fun SimpleCameraPreview(analyzer: ImageAnalysis.Analyzer) {
//    val lifecycleOwner = LocalLifecycleOwner.current
//    val context = LocalContext.current
//    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
//
//    AndroidView(
//        factory = { ctx ->
//            val preview = PreviewView(ctx)
//            val executor = ContextCompat.getMainExecutor(ctx)
//            cameraProviderFuture.addListener({
//                val cameraProvider = cameraProviderFuture.get()
//                bindPreview(
//                    lifecycleOwner,
//                    preview,
//                    cameraProvider,
//                    analyzer,
//                    executor
//                )
//            }, executor)
//            preview
//        },
//        modifier = Modifier.fillMaxSize(),
//    )
//}
//
//@SuppressLint("UnsafeExperimentalUsageError")
//private fun bindPreview(
//    lifecycleOwner: LifecycleOwner,
//    previewView: PreviewView,
//    cameraProvider: ProcessCameraProvider,
//    analyzer: ImageAnalysis.Analyzer,
//    executor: Executor
//) {
//    val preview = Preview.Builder().build().also {
//        it.setSurfaceProvider(previewView.surfaceProvider)
//    }
//
//    val cameraSelector = CameraSelector.Builder()
//        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
//        .build()
//
//    cameraProvider.unbindAll()
//    cameraProvider.bindToLifecycle(
//        lifecycleOwner,
//        cameraSelector,
//        setupImageAnalysis(executor, analyzer),
//        preview
//    )
//}
//
//private fun setupImageAnalysis(executor: Executor, analyzer: ImageAnalysis.Analyzer): ImageAnalysis {
//    return ImageAnalysis.Builder()
//        .setTargetResolution(Size(720, 1280))
//        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
//        .build()
//        .apply {
//            setAnalyzer(executor,analyzer)
//        }
//}

