
package com.finadapt.adaptivefinance.feature.gamification
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
import kotlin.math.abs

data class MapStop(
    val xpRequired: Int,
    val title: String,
    val isBuilding: Boolean,
    val swingOffset: Float
)

@Composable
fun RewardsScreen(
    userXp: Int,
    userCoins: Int,
    shieldCount: Int,
    isDarkMode: Boolean = false, // 🟢 Respects global toggle!
    onBuyShield: () -> Unit,
) {
    // 🟢 DYNAMIC THEME COLORS
    // Light Mode: Soft Sky Blue to Light Slate. Dark Mode: Deep Midnight.
    val bgColorTop = if (isDarkMode) Color(0xFF0F172A) else Color(0xFFE0F2FE)
    val bgColorBottom = if (isDarkMode) Color(0xFF1E293B) else Color(0xFFF1F5F9)
    val textColor = if (isDarkMode) Color(0xFFF1F5F9) else Color(0xFF0F172A)
    val subTextColor = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B)
    if (isDarkMode) Color(0xFF1E293B) else Color.White

    val mapStops = listOf(
        MapStop(0, "Base Camp", isBuilding = true, swingOffset = 0f),
        MapStop(100, "Rocky Ridge", isBuilding = false, swingOffset = -0.35f),
        MapStop(250, "The First Gate", isBuilding = true, swingOffset = 0.3f),
        MapStop(500, "Crystal Cavern", isBuilding = false, swingOffset = -0.2f),
        MapStop(1000, "Skybridge", isBuilding = true, swingOffset = 0.4f),
        MapStop(1500, "Frozen Falls", isBuilding = false, swingOffset = -0.42f),
        MapStop(2000, "Dragon's Peak", isBuilding = true, swingOffset = 0.25f),
        MapStop(2500, "Cloud Temple", isBuilding = false, swingOffset = -0.3f),
        MapStop(3500, "Astral Observatory", isBuilding = true, swingOffset = 0.35f),
        MapStop(5000, "Summit of Prosperity", isBuilding = true, swingOffset = 0f)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(bgColorTop, bgColorBottom)))
    ) {
        // 1. 🗺️ THE SCROLLABLE MOUNTAIN MAP
        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(180.dp))

                Text("The Ascent", fontSize = 36.sp, fontWeight = FontWeight.Black, color = textColor, letterSpacing = 2.sp)
                Text("Lifetime Power: $userXp XP", color = Color(0xFF10B981), fontSize = 16.sp, fontWeight = FontWeight.Bold)

                Spacer(modifier = Modifier.height(40.dp))

                MountainRoadmap(userXp = userXp, mapStops = mapStops, isDarkMode = isDarkMode)

                Spacer(modifier = Modifier.height(100.dp))
            }
        }

        // 2. 🛡️ THE STICKY HUD (Adapts to Light/Dark Mode)
        Surface(
            color = bgColorTop.copy(alpha = 0.95f),
            modifier = Modifier.fillMaxWidth(),
            shadowElevation = if (isDarkMode) 0.dp else 4.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // COIN INVENTORY
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(40.dp).background(Color(0xFFF59E0B).copy(alpha = 0.2f), CircleShape).border(1.dp, Color(0xFFF59E0B), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🪙", fontSize = 18.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Coins", color = subTextColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text("$userCoins", color = textColor, fontSize = 18.sp, fontWeight = FontWeight.Black)
                        }
                    }

                    // SHIELD INVENTORY
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Shields", color = subTextColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text("x$shieldCount", color = textColor, fontSize = 18.sp, fontWeight = FontWeight.Black)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Box(
                            modifier = Modifier.size(40.dp).background(Color(0xFF06B6D4).copy(alpha = 0.2f), CircleShape).border(1.dp, Color(0xFF06B6D4), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Shield, contentDescription = "Shields", tint = Color(0xFF06B6D4), modifier = Modifier.size(20.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                val shieldCost = 500
                val canAfford = userCoins >= shieldCost

                Button(
                    onClick = onBuyShield,
                    enabled = canAfford,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF10B981),
                        disabledContainerColor = if (isDarkMode) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Text(
                        text = if (canAfford) "Buy Shield (-$shieldCost 🪙)" else "Need $shieldCost 🪙 for Shield",
                        fontWeight = FontWeight.Bold,
                        color = if (canAfford) Color.White else subTextColor
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────
// ⛰️ THE MOUNTAIN TRAIL ROADMAP
// ─────────────────────────────────────────────────────────
@Composable
fun MountainRoadmap(userXp: Int, mapStops: List<MapStop>, isDarkMode: Boolean) {
    val nodeSpacing = 150.dp
    val mapHeight = nodeSpacing * mapStops.size

    // 🟢 DYNAMIC TRAIL COLORS
    val trailLockedColor = if (isDarkMode) Color(0xFF334155) else Color(0xFFCBD5E1)
    val trailUnlockedColor = Color(0xFF10B981) // Adaptive Finance Emerald Green!
    val nodeLabelColor = if (isDarkMode) Color(0xFF1E293B) else Color.White
    val textColor = if (isDarkMode) Color.White else Color(0xFF0F172A)

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.15f,
        animationSpec = infiniteRepeatable(animation = tween(1200, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
        label = "scale"
    )

    BoxWithConstraints(modifier = Modifier.fillMaxWidth().height(mapHeight)) {
        val widthPx = maxWidth.value

        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 20.dp.toPx()

            val nodeCenters = mapStops.mapIndexed { index, stop ->
                val x = (size.width / 2) + (size.width * stop.swingOffset)
                val y = size.height - (index * nodeSpacing.toPx()) - (nodeSpacing.toPx() / 2)
                Offset(x, y)
            }

            // Locked Path
            val lockedPath = Path().apply {
                moveTo(nodeCenters.first().x, nodeCenters.first().y)
                for (i in 0 until nodeCenters.size - 1) {
                    val curr = nodeCenters[i]
                    val next = nodeCenters[i + 1]
                    val controlOffset = abs(next.y - curr.y) * 0.4f
                    cubicTo(curr.x, curr.y - controlOffset, next.x, next.y + controlOffset, next.x, next.y)
                }
            }
            drawPath(path = lockedPath, color = trailLockedColor, style = Stroke(width = strokeWidth, cap = StrokeCap.Round))
            drawPath(path = lockedPath, color = Color.Black.copy(alpha = 0.15f), style = Stroke(width = strokeWidth * 0.3f, cap = StrokeCap.Round))

            // Unlocked Green Path
            val highestUnlockedIndex = mapStops.indexOfLast { userXp >= it.xpRequired }
            if (highestUnlockedIndex > 0) {
                val unlockedPath = Path().apply {
                    moveTo(nodeCenters.first().x, nodeCenters.first().y)
                    for (i in 0 until highestUnlockedIndex) {
                        val curr = nodeCenters[i]
                        val next = nodeCenters[i + 1]
                        val controlOffset = abs(next.y - curr.y) * 0.4f
                        cubicTo(curr.x, curr.y - controlOffset, next.x, next.y + controlOffset, next.x, next.y)
                    }
                }
                drawPath(path = unlockedPath, color = trailUnlockedColor, style = Stroke(width = strokeWidth, cap = StrokeCap.Round))
                drawPath(path = unlockedPath, color = Color.White.copy(alpha = 0.3f), style = Stroke(width = strokeWidth * 0.3f, cap = StrokeCap.Round))
            }
        }

        mapStops.forEachIndexed { index, stop ->
            val isUnlocked = userXp >= stop.xpRequired
            val isCurrent = userXp >= stop.xpRequired && (index == mapStops.size - 1 || userXp < mapStops[index + 1].xpRequired)

            val offsetX = (widthPx * stop.swingOffset).dp
            val offsetY = mapHeight - (index * nodeSpacing) - (nodeSpacing / 2) - 45.dp

            Box(
                modifier = Modifier
                    .offset(x = (widthPx / 2).dp + offsetX - 45.dp, y = offsetY)
                    .size(90.dp),
                contentAlignment = Alignment.Center
            ) {
                val nodeSize = if (stop.isBuilding) 80.dp else 60.dp
                val shape = if (stop.isBuilding) RoundedCornerShape(16.dp) else CircleShape

                // The Node
                Box(
                    modifier = Modifier
                        .size(nodeSize)
                        .scale(if (isCurrent) pulseScale else 1f)
                        .shadow(if (isCurrent) 12.dp else 4.dp, shape, spotColor = if (isCurrent) trailUnlockedColor else Color.Black)
                        .background(if (isUnlocked) trailUnlockedColor else trailLockedColor, shape)
                        .border(width = if (isCurrent) 4.dp else 0.dp, color = if (isCurrent) Color.White else Color.Transparent, shape = shape),
                    contentAlignment = Alignment.Center
                ) {
                    if (!isUnlocked) {
                        Icon(Icons.Default.Lock, contentDescription = "Locked", tint = if (isDarkMode) Color(0xFF64748B) else Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text(if (stop.isBuilding) "🏯" else "🏕️", fontSize = if (stop.isBuilding) 40.sp else 30.sp)
                    }
                }

                // Node Label Bubble
                Surface(
                    color = nodeLabelColor.copy(alpha = 0.95f),
                    shape = RoundedCornerShape(8.dp),
                    shadowElevation = if (isDarkMode) 0.dp else 4.dp,
                    border = BorderStroke(1.dp, if (isDarkMode) Color(0xFF334155) else Color(0xFFE2E8F0)),
                    modifier = Modifier.align(Alignment.BottomCenter).offset(y = 20.dp)
                ) {
                    Text(
                        text = stop.title,
                        color = textColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}