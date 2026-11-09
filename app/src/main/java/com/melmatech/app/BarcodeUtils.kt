package com.melmatech.app

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import java.util.UUID

/**
 * Codes internes MelmaTech.
 * Le préfixe MT identifie les références internes ;
 * ces codes ne sont pas des codes commerciaux GS1/EAN.
 */
object BarcodeUtils {
    fun genererCodeInterne(): String =
        "MT" + UUID.randomUUID().toString()
            .replace("-", "")
            .uppercase()
            .take(12)
}

@Composable
fun BarcodeCode128(value: String) {
    if (value.isBlank()) return

    val bitmap = remember(value) {
        try {
            val matrix = MultiFormatWriter().encode(
                value,
                BarcodeFormat.CODE_128,
                640,
                160
            )

            Bitmap.createBitmap(
                matrix.width,
                matrix.height,
                Bitmap.Config.ARGB_8888
            ).also { image ->
                for (x in 0 until matrix.width) {
                    for (y in 0 until matrix.height) {
                        image.setPixel(
                            x,
                            y,
                            if (matrix.get(x, y)) Color.BLACK else Color.WHITE
                        )
                    }
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Code-barres $value",
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
        )
    }
}
