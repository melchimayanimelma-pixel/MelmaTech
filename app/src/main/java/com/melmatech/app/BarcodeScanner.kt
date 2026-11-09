package com.melmatech.app

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@Composable
fun BarcodeScannerDialog(
    onDismiss: () -> Unit,
    onBarcodeDetected: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var permissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var permissionAsked by remember { mutableStateOf(false) }
    DisposableEffect(permissionGranted) {
        onDispose {
            if (permissionGranted) {
                try {
                    ProcessCameraProvider.getInstance(context).get().unbindAll()
                } catch (_: Exception) {
                }
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> permissionGranted = granted }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Scanner un code-barres") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (!permissionGranted) {
                    Text("MelmaTech a besoin de la caméra pour lire le code-barres.")
                    Button(onClick = {
                        permissionAsked = true
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }) { Text("Autoriser la caméra") }
                    if (permissionAsked) {
                        Text("Si l'accès a été refusé, autorisez la caméra dans les paramètres Android.")
                    }
                } else {
                    AndroidView(
                        modifier = Modifier.fillMaxWidth().height(300.dp),
                        factory = { ctx ->
                            val previewView = PreviewView(ctx)
                            val cameraProviderFuture =
                                ProcessCameraProvider.getInstance(ctx)
                            val executor = Executors.newSingleThreadExecutor()
                            cameraProviderFuture.addListener({
                                val provider = cameraProviderFuture.get()
                                val preview = Preview.Builder().build().also {
                                    it.setSurfaceProvider(previewView.surfaceProvider)
                                }
                                val scanner = BarcodeScanning.getClient()
                                val analysis = ImageAnalysis.Builder()
                                    .setBackpressureStrategy(
                                        ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
                                    ).build()
                                analysis.setAnalyzer(executor) { proxy ->
                                    val mediaImage = proxy.image
                                    if (mediaImage == null) {
                                        proxy.close()
                                    } else {
                                        val image = InputImage.fromMediaImage(
                                            mediaImage, proxy.imageInfo.rotationDegrees
                                        )
                                        scanner.process(image)
                                            .addOnSuccessListener { codes ->
                                                val value = codes.firstOrNull {
                                                    !it.rawValue.isNullOrBlank()
                                                }?.rawValue
                                                if (!value.isNullOrBlank()) {
                                                    onBarcodeDetected(value)
                                                }
                                            }
                                            .addOnCompleteListener { proxy.close() }
                                    }
                                }
                                try {
                                    provider.unbindAll()
                                    provider.bindToLifecycle(
                                        lifecycleOwner,
                                        CameraSelector.DEFAULT_BACK_CAMERA,
                                        preview,
                                        analysis
                                    )
                                } catch (_: Exception) {
                                }
                            }, ContextCompat.getMainExecutor(ctx))
                            previewView
                        }
                    )
                    Text("Placez le code dans le cadre de la caméra.")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Fermer") }
        }
    )
}
