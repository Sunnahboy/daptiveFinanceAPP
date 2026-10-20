package com.finadapt.adaptivefinance.ui.components

import android.annotation.SuppressLint
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AnimatedDraggableFab(
    text: String,
    icon: ImageVector,
    contentDescription: String,
    bubbleColor: Color,
    buttonContainerColor: Color,
    buttonContentColor: Color,
    delayMillis: Long = 1500L,
    onClick: () -> Unit,
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier
) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    var displayedText by remember { mutableStateOf("") }
    var isLabelVisible by remember { mutableStateOf(false) }

    val alpha by animateFloatAsState(targetValue = if (isDragging) 0.5f else 1f, label = "fab_alpha")

    LaunchedEffect(Unit) {
        delay(delayMillis)
        isLabelVisible = true
        text.forEachIndexed { index, _ ->
            displayedText = text.substring(0, index + 1)
            delay(80)
        }
        delay(3000)
        isLabelVisible = false
    }

    Box(
        modifier = modifier
            .graphicsLayer(alpha = alpha)
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { isDragging = true },
                    onDragEnd = {
                        isDragging = false
                        // Snap to nearest horizontal edge
                        offsetX = if (offsetX > 0f) 0f else 0f
                    },
                    onDragCancel = { isDragging = false }
                ) { change, dragAmount ->
                    change.consume()
                    offsetX += dragAmount.x
                    offsetY += dragAmount.y
                    // Clamp to prevent covering status bar or bottom nav
                    offsetY = offsetY.coerceIn(-400f, 50f)
                    offsetX = offsetX.coerceIn(-250f, 250f)
                }
            }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End,
            modifier = Modifier.combinedClickable(
                onClick = onClick,
                onLongClick = { isLabelVisible = !isLabelVisible }
            )
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
