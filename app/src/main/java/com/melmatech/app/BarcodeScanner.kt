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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

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
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var permissionAsked by remember { mutableStateOf(false) }

    val executor = remember { Executors.newSingleThreadExecutor() }
    val detectionHandled = remember { AtomicBoolean(false) }
    val scannerOptions = remember {
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E,
                Barcode.FORMAT_CODE_128
            )
            .build()
    }
    val scanner = remember { BarcodeScanning.getClient(scannerOptions) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            try {
                cameraProvider?.unbindAll()
            } catch (_: Exception) {
            }
            scanner.close()
            executor.shutdown()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionGranted = granted
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Scanner un code-barres") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (!permissionGranted) {
                    Text("MelmaTech a besoin de la caméra pour lire le code-barres.")
                    Button(
                        onClick = {
                            permissionAsked = true
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    ) {
                        Text("Autoriser la caméra")
                    }
                    if (permissionAsked) {
                        Text("Si l'accès a été refusé, autorisez la caméra dans les paramètres Android.")
                    }
                } else {
                    AndroidView(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        factory = { ctx ->
                            val previewView = PreviewView(ctx)
                            val cameraProviderFuture =
                                ProcessCameraProvider.getInstance(ctx)

                            cameraProviderFuture.addListener({
                                try {
                                    val provider = cameraProviderFuture.get()
                                    cameraProvider = provider

                                    val preview = Preview.Builder()
                                        .build()
                                        .also {
                                            it.setSurfaceProvider(previewView.surfaceProvider)
                                        }

                                    val analysis = ImageAnalysis.Builder()
                                        .setBackpressureStrategy(
                                            ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
                                        )
                                        .build()

                                    analysis.setAnalyzer(executor) { proxy ->
                                        val mediaImage = proxy.image
                                        if (mediaImage == null || detectionHandled.get()) {
                                            proxy.close()
                                        } else {
                                            val inputImage = InputImage.fromMediaImage(
                                                mediaImage,
                                                proxy.imageInfo.rotationDegrees
                                            )

                                            scanner.process(inputImage)
                                                .addOnSuccessListener { codes ->
                                                    val value = codes.firstOrNull {
                                                        !it.rawValue.isNullOrBlank()
                                                    }?.rawValue

                                                    if (!value.isNullOrBlank() &&
                                                        detectionHandled.compareAndSet(false, true)
                                                    ) {
                                                        onBarcodeDetected(value)
                                                    }
                                                }
                                                .addOnCompleteListener {
                                                    proxy.close()
                                                }
                                        }
                                    }

                                    provider.unbindAll()
                                    provider.bindToLifecycle(
                                        lifecycleOwner,
                                        CameraSelector.DEFAULT_BACK_CAMERA,
                                        preview,
                                        analysis
                                    )
                                } catch (_: Exception) {
                                    // La fermeture du dialogue permet de quitter le scanner
                                    // si la caméra n'est pas disponible.
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
            TextButton(onClick = onDismiss) {
                Text("Fermer")
            }
        }
    )
}
