package com.rifsxd.ksunext.ui.theme

import android.graphics.Color.HSVToColor
import android.graphics.Color.colorToHSV
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

@Composable
fun ColorPickerDialog(
    initialColor: Color,
    onDismissRequest: () -> Unit,
    onColorSelected: (Color) -> Unit
) {
    var currentColor by remember { mutableStateOf(initialColor) }
    var hsv by remember { 
        mutableStateOf(
            FloatArray(3).apply { 
                colorToHSV(initialColor.toArgb(), this) 
            }
        ) 
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Pick a Color") },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Preview
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(currentColor)
                        .border(1.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                )

                // Saturation/Value Box
                SatValBox(
                    hue = hsv[0],
                    saturation = hsv[1],
                    value = hsv[2],
                    onSatValChange = { s, v ->
                        hsv[1] = s
                        hsv[2] = v
                        currentColor = Color(HSVToColor(hsv))
                    }
                )

                // Hue Slider
                HueSlider(
                    hue = hsv[0],
                    onHueChange = { h ->
                        hsv[0] = h
                        currentColor = Color(HSVToColor(hsv))
                    }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onColorSelected(currentColor) }) {
                Text("Select")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun HueSlider(
    hue: Float,
    onHueChange: (Float) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp)
            .clip(MaterialTheme.shapes.extraSmall)
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    change.consume()
                    val newHue = (change.position.x / size.width) * 360f
                    onHueChange(newHue.coerceIn(0f, 360f))
                }
            }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val newHue = (offset.x / size.width) * 360f
                    onHueChange(newHue.coerceIn(0f, 360f))
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val colors = listOf(
                Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red
            )
            drawRect(
                brush = Brush.horizontalGradient(colors)
            )
            // Draw thumb
            val x = (hue / 360f) * size.width
            drawCircle(
                color = Color.White,
                radius = size.height / 2,
                center = Offset(x, size.height / 2),
            )
            drawCircle(
                color = Color.Black,
                radius = size.height / 2,
                center = Offset(x, size.height / 2),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
            )
        }
    }
}

@Composable
fun SatValBox(
    hue: Float,
    saturation: Float,
    value: Float,
    onSatValChange: (Float, Float) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(MaterialTheme.shapes.extraSmall)
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    change.consume()
                    val newSat = (change.position.x / size.width).coerceIn(0f, 1f)
                    val newVal = 1f - (change.position.y / size.height).coerceIn(0f, 1f)
                    onSatValChange(newSat, newVal)
                }
            }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val newSat = (offset.x / size.width).coerceIn(0f, 1f)
                    val newVal = 1f - (offset.y / size.height).coerceIn(0f, 1f)
                    onSatValChange(newSat, newVal)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Draw Hue background
            drawRect(color = Color(HSVToColor(floatArrayOf(hue, 1f, 1f))))
            
            // Draw Saturation gradient (Left to Right, White to Transparent)
            // Wait, Saturation is usually White -> Color (Left to Right) if using the S-V square?
            // Actually, S-V square:
            // Top-Left: White (S=0, V=1)
            // Top-Right: Color (S=1, V=1)
            // Bottom-Left: Black (S=0, V=0)
            // Bottom-Right: Black (S=1, V=0)
            
            // So: Horizontal gradient from White to Transparent (assuming underlying color is the Hue)
            // NO. 
            // Layer 1: Hue Color (solid)
            // Layer 2: Horizontal Gradient (White -> Transparent) -- this makes Left side White (S=0)
            // Layer 3: Vertical Gradient (Transparent -> Black) -- this makes Bottom side Black (V=0)
            
            // Let's verify:
            // Top-Right: Hue (S=1), White transparent, Black transparent -> Hue. Correct.
            // Top-Left: Hue, White opaque (overwrites hue), Black transparent -> White. Correct.
            // Bottom-Left: White, Black opaque -> Black. Correct.
            
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.White, Color.Transparent)
                )
            )
            
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black)
                )
            )

            // Draw Thumb
            val x = saturation * size.width
            val y = (1f - value) * size.height
            drawCircle(
                color = Color.White,
                radius = 8.dp.toPx(),
                center = Offset(x, y)
            )
            drawCircle(
                color = Color.Black,
                radius = 8.dp.toPx(),
                center = Offset(x, y),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
            )
        }
    }
}
