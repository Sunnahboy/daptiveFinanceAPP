package com.finadapt.adaptivefinance.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun AnimatedDraggableFab(
    text: String,
    icon: ImageVector,
    contentDescription: String,
    bubbleColor: Color,
    buttonContainerColor: Color,
    buttonContentColor: Color,
    delayMillis: Long = 1500L, // Allows us to stagger the animations
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    var displayedText by remember { mutableStateOf("") }
    var isLabelVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(delayMillis)
        isLabelVisible = true
        text.forEachIndexed { index, _ ->
            displayedText = text.substring(0, index + 1)
            delay(80)
        }
        delay(4000)
        isLabelVisible = false
    }

    Box(
        modifier = modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    offsetX += dragAmount.x
                    offsetY += dragAmount.y
                }
            }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            // THE SPEECH BUBBLE
            AnimatedVisibility(
                visible = isLabelVisible && displayedText.isNotEmpty(),
                enter = fadeIn() + expandHorizontally(),
                exit = fadeOut() + shrinkHorizontally()
            ) {
                Surface(
                    color = bubbleColor,
                    shape = RoundedCornerShape(20.dp, 20.dp, 0.dp, 20.dp),
                    modifier = Modifier.padding(end = 8.dp),
                    shadowElevation = 6.dp
                ) {
                    Text(
                        text = displayedText,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }

            // THE MAIN BUTTON
            Surface(
                onClick = onClick,
                modifier = Modifier.size(56.dp),
                shape = CircleShape,
                color = buttonContainerColor,
                shadowElevation = 8.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = contentDescription,
                        tint = buttonContentColor,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}