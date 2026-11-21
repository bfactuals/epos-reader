package com.example.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Custom 0 KB procedural Fanned-Pages Logo:
 * Rich off-black (#121214) rounded squircle with subtle matte border, containing a soft off-white
 * (#F7F5F0) open-book base with a curved center spine and 5 slender, gently tapered pages
 * fanning upward from left to right with softly fading opacity.
 */
@Composable
fun FolioLogoMark(
    size: Dp = 36.dp,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val s = minOf(w, h)
        val radius = s * 0.24f

        // 1. Rich off-black (#121214) rounded squircle
        drawRoundRect(
            color = Color(0xFF121214),
            topLeft = Offset.Zero,
            size = Size(w, h),
            cornerRadius = CornerRadius(radius, radius)
        )
        // Subtle matte border
        drawRoundRect(
            color = Color(0xFF2D2D33),
            topLeft = Offset(s * 0.02f, s * 0.02f),
            size = Size(w - s * 0.04f, h - s * 0.04f),
            cornerRadius = CornerRadius(radius * 0.96f, radius * 0.96f),
            style = Stroke(width = s * 0.028f)
        )

        val offWhite = Color(0xFFF7F5F0)
        val spineX = w * 0.50f
        val spineBaseY = h * 0.74f

        // 2. Five slender, gently tapered pages fanning upward from left to right with fading opacity
        val pageAlphas = floatArrayOf(0.98f, 0.82f, 0.65f, 0.47f, 0.30f)
        // Tip endpoints fanning gracefully left -> right
        val tips = arrayOf(
            Offset(w * 0.22f, h * 0.37f),
            Offset(w * 0.35f, h * 0.27f),
            Offset(w * 0.50f, h * 0.22f),
            Offset(w * 0.66f, h * 0.27f),
            Offset(w * 0.79f, h * 0.37f)
        )
        val ctrlOffsets = arrayOf(
            Offset(w * 0.34f, h * 0.56f),
            Offset(w * 0.41f, h * 0.49f),
            Offset(w * 0.50f, h * 0.46f),
            Offset(w * 0.59f, h * 0.49f),
            Offset(w * 0.66f, h * 0.56f)
        )

        for (i in 0 until 5) {
            val tip = tips[i]
            val ctrl = ctrlOffsets[i]
            // Slender tapered leaf polygon curving from spine pivot up to narrow tip
            val dx = tip.x - spineX
            val dy = tip.y - spineBaseY
            val len = kotlin.math.hypot(dx.toDouble(), dy.toDouble()).toFloat().coerceAtLeast(1f)
            val perpX = -dy / len
            val perpY = dx / len
            val halfBase = s * 0.024f
            val halfMid = s * 0.020f

            val leaf = Path().apply {
                moveTo(spineX - perpX * halfBase, spineBaseY - perpY * halfBase)
                quadraticTo(
                    ctrl.x - perpX * halfMid,
                    ctrl.y - perpY * halfMid,
                    tip.x,
                    tip.y
                )
                quadraticTo(
                    ctrl.x + perpX * halfMid,
                    ctrl.y + perpY * halfMid,
                    spineX + perpX * halfBase,
                    spineBaseY + perpY * halfBase
                )
                close()
            }
            drawPath(path = leaf, color = offWhite.copy(alpha = pageAlphas[i]))
        }

        // 3. Soft off-white (#F7F5F0) open-book base with curved center spine
        val baseStroke = Stroke(
            width = s * 0.056f,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
        val openBookBase = Path().apply {
            moveTo(w * 0.18f, h * 0.69f)
            quadraticTo(w * 0.34f, h * 0.63f, spineX, spineBaseY + h * 0.04f)
            quadraticTo(w * 0.66f, h * 0.63f, w * 0.82f, h * 0.69f)
        }
        drawPath(path = openBookBase, color = offWhite, style = baseStroke)
    }
}
