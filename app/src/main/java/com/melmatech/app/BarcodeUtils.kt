package com.melmatech.app

import java.util.UUID
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp

/**
 * Codes internes MelmaTech. Le préfixe MT identifie les références internes ;
 * ces codes ne sont pas des codes commerciaux GS1/EAN.
 */
object BarcodeUtils {
    fun genererCodeInterne(): String =
        "MT" + UUID.randomUUID().toString()
            .replace("-", "")
            .uppercase()
            .take(12)
}


@androidx.compose.runtime.Composable
fun BarcodeCode128(value: String) {
    if (value.isBlank()) return
    val bitmap = androidx.compose.runtime.remember(value) {
        try {
            val matrix = com.google.zxing.MultiFormatWriter().encode(
                value,
                com.google.zxing.BarcodeFormat.CODE_128,
                640,
                160
            )
            android.graphics.Bitmap.createBitmap(
                matrix.width,
                matrix.height,
                android.graphics.Bitmap.Config.ARGB_8888
            ).also { image ->
                for (x in 0 until matrix.width) {
                    for (y in 0 until matrix.height) {
                        image.setPixel(
                            x, y,
                            if (matrix.get(x, y)) android.graphics.Color.BLACK
                            else android.graphics.Color.WHITE
                        )
                    }
                }
            }
        } catch (_: Exception) {
            null
        }
    }
    if (bitmap != null) {
        androidx.compose.foundation.Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Code-barres $value",
            modifier = androidx.compose.ui.Modifier
                .fillMaxWidth()
                .height(80.dp)
        )
    }
}
