package com.finadapt.adaptivefinance.feature.gamification

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
import com.airbnb.lottie.compose.*
import com.finadapt.adaptivefinance.R
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Represents a single node on the gamified roadmap.
 */
data class MapStop(
    val xpRequired: Int,
    val title: String,
    val isBuilding: Boolean,
    val swingOffset: Float,
    val uniqueLottieRes: Int
)

/**
 * Core Gamification UI Layer.
 * * Theoretical Backing:
 * - Self-Determination Theory (SDT): Competence is built via the visual map progression.
 * - SDG 8 (Financial Resilience): The "Shield" mechanic simulates building an emergency fund.
 * - Long-term Habit Maintenance: The "Ascension" system prevents motivational decay by offering
 * permanent prestige multipliers once the initial goal is reached.
 */
@Composable
fun RewardsScreen(
    userXp: Int,
    userCoins: Int,
    shieldCount: Int,
    ascensionLevel: Int = 0, // Tracks Prestige/Loop count
    isDarkMode: Boolean = false,
    onBuyShield: () -> Unit,
    onAscend: () -> Unit // Triggered to reset map and gain multiplier
) {
    // Theme Colors
    val bgColorTop = if (isDarkMode) Color(0xFF0F172A) else Color(0xFFE0F2FE)
    val bgColorBottom = if (isDarkMode) Color(0xFF1E293B) else Color(0xFFF1F5F9)
    val textColor = if (isDarkMode) Color(0xFFF1F5F9) else Color(0xFF0F172A)
    val subTextColor = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B)

    // Dynamic Theme based on Prestige Level
    val prestigeColor = when (ascensionLevel) {
        0 -> Color(0xFF10B981) // Emerald Green (First Run)
        1 -> Color(0xFFF59E0B) // Gold (Ascension 1)
        2 -> Color(0xFF3B82F6) // Diamond Blue (Ascension 2)
        else -> Color(0xFF8B5CF6) // Mythic Purple (Ascension 3+)
    }

    val mapStops = listOf(
        MapStop(0, "Base Camp", isBuilding = true, swingOffset = 0f, uniqueLottieRes = R.raw.camp_node),
        MapStop(100, "Slime Ambush", isBuilding = false, swingOffset = -0.35f, uniqueLottieRes = R.raw.fight_slime),
        MapStop(250, "The First Gate", isBuilding = true, swingOffset = 0.3f, uniqueLottieRes = R.raw.boss_gate_node),
        MapStop(500, "Crystal Cavern", isBuilding = false, swingOffset = -0.2f, uniqueLottieRes = R.raw.crystal_glow),
        MapStop(1000, "Skybridge", isBuilding = true, swingOffset = 0.4f, uniqueLottieRes = R.raw.boss_gate_node),
        MapStop(1500, "Frozen Falls", isBuilding = false, swingOffset = -0.42f, uniqueLottieRes = R.raw.camp_node),
        MapStop(2000, "Dragon's Peak", isBuilding = true, swingOffset = 0.25f, uniqueLottieRes = R.raw.boss_gate_node),
        MapStop(2500, "Cloud Temple", isBuilding = false, swingOffset = -0.3f, uniqueLottieRes = R.raw.crystal_glow),
        MapStop(3500, "Astral Observatory", isBuilding = true, swingOffset = 0.35f, uniqueLottieRes = R.raw.boss_gate_node),
        MapStop(5000, "Summit of Prosperity", isBuilding = true, swingOffset = 0f, uniqueLottieRes = R.raw.boss_gate_node)
    )

    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(bgColorTop, bgColorBottom)))) {
        Box(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(180.dp))

                // Dynamic Title showing Prestige Status
                val titleText = if (ascensionLevel > 0) "The Ascent: Peak $ascensionLevel" else "The Ascent"
                Text(titleText, fontSize = 36.sp, fontWeight = FontWeight.Black, color = textColor, letterSpacing = 2.sp)
                Text("Lifetime Power: $userXp XP", color = prestigeColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)

                Spacer(modifier = Modifier.height(40.dp))

                MountainRoadmap(
                    userXp = userXp,
                    mapStops = mapStops,
                    isDarkMode = isDarkMode,
                    trailColor = prestigeColor,
                    onAscend = onAscend
                )

                Spacer(modifier = Modifier.height(100.dp))
            }
        }

        // Top Sticky Header (Coins & Shields)
        Surface(color = bgColorTop.copy(alpha = 0.95f), modifier = Modifier.fillMaxWidth(), shadowElevation = if (isDarkMode) 0.dp else 4.dp) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp).statusBarsPadding()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    // Coins Display
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(40.dp).background(Color(0xFFF59E0B).copy(alpha = 0.2f), CircleShape).border(1.dp, Color(0xFFF59E0B), CircleShape), contentAlignment = Alignment.Center) { Text("🪙", fontSize = 18.sp) }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Coins", color = subTextColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text("$userCoins", color = textColor, fontSize = 18.sp, fontWeight = FontWeight.Black)
                        }
                    }

                    // Shields Display (Emergency Fund Metaphor)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Shields", color = subTextColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text("x$shieldCount", color = textColor, fontSize = 18.sp, fontWeight = FontWeight.Black)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Box(modifier = Modifier.size(40.dp).background(Color(0xFF06B6D4).copy(alpha = 0.2f), CircleShape).border(1.dp, Color(0xFF06B6D4), CircleShape), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Shield, contentDescription = "Shields", tint = Color(0xFF06B6D4), modifier = Modifier.size(20.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Buy Shield Button
                val shieldCost = 500
                val canAfford = userCoins >= shieldCost
                Button(
                    onClick = onBuyShield,
                    enabled = canAfford,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = prestigeColor, // Matches their current prestige tier
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

/**
 * Handles the visual rendering of the spline path and node progression.
 */
@Composable
fun MountainRoadmap(
    userXp: Int,
    mapStops: List<MapStop>,
    isDarkMode: Boolean,
    trailColor: Color,
    onAscend: () -> Unit
) {
    val nodeSpacing = 160.dp
    val mapHeight = nodeSpacing * mapStops.size

    val trailLockedColor = if (isDarkMode) Color(0xFF334155) else Color(0xFFCBD5E1)
    val nodeLabelColor = if (isDarkMode) Color(0xFF1E293B) else Color.White
    val textColor = if (isDarkMode) Color.White else Color(0xFF0F172A)

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(animation = tween(1500, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
        label = "scale"
    )

    BoxWithConstraints(modifier = Modifier.fillMaxWidth().height(mapHeight)) {
        val widthPx = maxWidth.value

        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 20.dp.toPx()
            val nodeCenters = mapStops.mapIndexed { index, stop ->
                Offset((size.width / 2) + (size.width * stop.swingOffset), size.height - (index * nodeSpacing.toPx()) - (nodeSpacing.toPx() / 2))
            }

            // Draw Locked (Background) Path
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

            // Draw Unlocked (Active) Path
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
                drawPath(path = unlockedPath, color = trailColor, style = Stroke(width = strokeWidth, cap = StrokeCap.Round))
                drawPath(path = unlockedPath, color = Color.White.copy(alpha = 0.3f), style = Stroke(width = strokeWidth * 0.3f, cap = StrokeCap.Round))
            }
        }

        mapStops.forEachIndexed { index, stop ->
            val isUnlocked = userXp >= stop.xpRequired
            val isCurrent = userXp >= stop.xpRequired && (index == mapStops.size - 1 || userXp < mapStops[index + 1].xpRequired)

            val offsetX = (widthPx * stop.swingOffset).dp
            val offsetY = mapHeight - (index * nodeSpacing) - (nodeSpacing / 2)

            val coroutineScope = rememberCoroutineScope()
            val tapScale = remember { Animatable(1f) }

            Box(
                modifier = Modifier
                    .offset(x = (widthPx / 2).dp + offsetX - 75.dp, y = offsetY - 75.dp)
                    .width(150.dp)
                    .height(180.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {

                    val nodeSize = if (stop.isBuilding) 85.dp else 70.dp
                    val shape = if (stop.isBuilding) RoundedCornerShape(20.dp) else CircleShape

                    val lottieRes = if (!isUnlocked) R.raw.locked_node else stop.uniqueLottieRes
                    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(lottieRes))

                    val progress by animateLottieCompositionAsState(
                        composition = composition,
                        iterations = LottieConstants.IterateForever,
                        isPlaying = true
                    )

                    Box(
                        modifier = Modifier
                            .size(nodeSize)
                            .scale(if (isCurrent) pulseScale * tapScale.value else tapScale.value)
                            .shadow(if (isCurrent) 16.dp else 4.dp, shape, spotColor = if (isCurrent) trailColor else Color.Black)
                            .background(if (isUnlocked) trailColor else trailLockedColor, shape)
                            .border(width = if (isCurrent) 4.dp else 0.dp, color = if (isCurrent) Color.White else Color.Transparent, shape = shape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                coroutineScope.launch {
                                    tapScale.animateTo(targetValue = 1.3f, animationSpec = tween(100, easing = FastOutSlowInEasing))
                                    tapScale.animateTo(targetValue = 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        LottieAnimation(composition = composition, progress = { progress }, modifier = Modifier.fillMaxSize(0.65f))

                        if (isCurrent) {
                            // PRESTIGE LOGIC: If they are at the very top, offer the Ascension Button
                            if (index == mapStops.size - 1) {
                                Button(
                                    onClick = onAscend,
                                    colors = ButtonDefaults.buttonColors(containerColor = trailColor),
                                    shape = RoundedCornerShape(20.dp),
                                    modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        .offset(y = (-45).dp)
                                        .shadow(8.dp, RoundedCornerShape(20.dp))
                                ) {
                                    Text(
                                        text = "ASCEND\n(+1.5x Boost)",
                                        fontWeight = FontWeight.Black,
                                        color = Color.White,
                                        textAlign = TextAlign.Center,
                                        fontSize = 10.sp
                                    )
                                }
                            } else {
                                // Standard Walking Avatar
                                val avatarComposition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.walking_character))
                                val avatarProgress by animateLottieCompositionAsState(composition = avatarComposition, iterations = LottieConstants.IterateForever, isPlaying = true)
                                LottieAnimation(
                                    composition = avatarComposition,
                                    progress = { avatarProgress },
                                    modifier = Modifier.size(55.dp).align(Alignment.TopCenter).offset(y = (-35).dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        color = nodeLabelColor.copy(alpha = 0.95f), shape = RoundedCornerShape(10.dp), shadowElevation = if (isDarkMode) 0.dp else 4.dp,
                        border = BorderStroke(1.dp, if (isDarkMode) Color(0xFF334155) else Color(0xFFE2E8F0))
                    ) {
                        Text(text = stop.title, color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp))
                    }
                }
            }
        }
    }
}