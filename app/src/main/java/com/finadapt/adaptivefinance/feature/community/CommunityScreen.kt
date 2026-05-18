package com.finadapt.adaptivefinance.feature.community

import android.media.MediaPlayer
import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.airbnb.lottie.compose.*
import com.finadapt.adaptivefinance.R
import com.finadapt.adaptivefinance.data.remote.LeaderboardEntry
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar

// ============================================================================
// 1. MAIN SCREEN ORCHESTRATOR
// ============================================================================
@Composable
fun CommunityScreen(
    leaderboardData: List<LeaderboardEntry>,
    currentAnonName: String,
    isLoading: Boolean,
    isDarkMode: Boolean = false,
    onCheerClicked: (String, String) -> Unit,
    hallOfFameData: List<LeaderboardEntry> = emptyList(),
    spendableCoins: Int = 0,
    shieldCount: Int = 0
) {
    val bgColor = if (isDarkMode) Color(0xFF0F172A) else Color(0xFFE2E8F0)
    val listContainerColor = if (isDarkMode) Color(0xFF1E293B).copy(alpha = 0.8f) else Color.White.copy(alpha = 0.6f)
    val textColor = if (isDarkMode) Color.White else Color(0xFF0F172A)
    val subTextColor = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B)

    //state to trigger our custom alert
    var showBrokeAlert by remember { mutableStateOf(false) }

    //Auto hide the alert after 2.5 seconds
    LaunchedEffect(showBrokeAlert) {
        if (showBrokeAlert) {
            delay(2500)
            showBrokeAlert = false
        }
    }

    //Wrap everything in a Box so the alert can float on top
    Box(modifier = Modifier.fillMaxSize()) {
        if (isLoading) {
            LoadingState(bgColor)
        } else if (leaderboardData.isEmpty()) {
            EmptyState(bgColor, subTextColor)
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().background(bgColor),
                contentPadding = PaddingValues(top = 48.dp, bottom = 120.dp)
            ) {
                // 1. Hall of Fame
                if (hallOfFameData.isNotEmpty()) {
                    item { HallOfFameSection(hallOfFameData, textColor, subTextColor) }
                }

                // 2. Podium
                item { PodiumSection(leaderboardData.take(3), textColor) }

                // 3. League Info Banner
                item {
                    LeagueInfoBanner(
                        topPlayer = leaderboardData.firstOrNull(),
                        currentAnonName = currentAnonName,
                        spendableCoins = spendableCoins,
                        shieldCount = shieldCount, // NEW: Pass to banner
                        textColor = textColor
                    )
                }

                // 4. List Header
                item { LeaderboardListHeader(listContainerColor, subTextColor) }

                // 5. The Players List (Grouped by Tier)
                val grouped = leaderboardData.groupBy { it.tier }
                val tierOrder = listOf("Platinum Legend", "Gold Master", "Silver Guardian", "Bronze Novice")

                tierOrder.forEach { tier ->
                    val players = grouped[tier] ?: return@forEach

                    item { TierDivider(tier, listContainerColor) }

                    itemsIndexed(items = players, key = { _, p -> p.userId }) { index, player ->
                        val isMe = player.anonymousName == currentAnonName
                        LeaderboardPlayerRow(
                            player = player,
                            rank = index + 1,
                            isMe = isMe,
                            listContainerColor = listContainerColor,
                            textColor = textColor,
                            subTextColor = subTextColor,
                            spendableCoins = spendableCoins,
                            onCheerClicked = onCheerClicked,
                            onBroke = { showBrokeAlert = true }
                        )
                    }
                }

                // 6. List Footer
                item { LeaderboardListFooter(listContainerColor) }
            }
        }

        // THE CUSTOM POPUP
        //will slide down from the top of the screen
        AnimatedVisibility(
            visible = showBrokeAlert,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 32.dp) //Pushes it down below the status bar
                .zIndex(50f)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFEF4444), // Danger Red color
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("😢", fontSize = 22.sp) // The crying emoji!
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Not enough coins! Need 10 \uD83D\uDCB0",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}


// ============================================================================
// 2. MODULAR UI COMPONENTS (The "Lego Bricks")
// ============================================================================

@Composable
private fun LoadingState(bgColor: Color) {
    Box(modifier = Modifier.fillMaxSize().background(bgColor), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Color(0xFF0284C7))
    }
}

@Composable
private fun EmptyState(bgColor: Color, subTextColor: Color) {
    Box(modifier = Modifier.fillMaxSize().background(bgColor), contentAlignment = Alignment.Center) {
        Text("Leaderboard is empty or offline.", color = subTextColor)
    }
}

@Composable
private fun HallOfFameSection(hallOfFameData: List<LeaderboardEntry>, textColor: Color, subTextColor: Color) {
    Text(
        "🏆 Hall of Fame (Last Week)",
        modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 4.dp),
        style = MaterialTheme.typography.titleSmall,
        color = subTextColor,
        fontWeight = FontWeight.Bold
    )
    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(hallOfFameData) { winner ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(64.dp)) {
                Box(
                    modifier = Modifier.size(50.dp).background(Color(0xFFFDE047).copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("👑", fontSize = 20.sp)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(winner.anonymousName, fontSize = 10.sp, color = textColor, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                Text("${winner.xp} XP", fontSize = 9.sp, color = subTextColor, textAlign = TextAlign.Center)
            }
        }
    }
    HorizontalDivider(color = subTextColor.copy(alpha = 0.15f), modifier = Modifier.padding(horizontal = 16.dp))
}

@Composable
private fun PodiumSection(top3: List<LeaderboardEntry>, textColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        if (top3.size > 1) PodiumCard(top3[1], "2nd", 130.dp, Color(0xFFCBD5E1), Color(0xFF94A3B8), textColor)
        if (top3.isNotEmpty()) PodiumCard(top3[0], "1st", 170.dp, Color(0xFFFDE047), Color(0xFFEAB308), textColor)
        if (top3.size > 2) PodiumCard(top3[2], "3rd", 100.dp, Color(0xFFFDBA74), Color(0xFFD97706), textColor)
    }
}

@Composable
private fun LeagueInfoBanner(
    topPlayer: LeaderboardEntry?, 
    currentAnonName: String, 
    spendableCoins: Int, 
    shieldCount: Int,
    textColor: Color
) {
    val daysUntilReset = remember {
        val calendar = Calendar.getInstance()
        val currentDay = calendar.get(Calendar.DAY_OF_WEEK)
        if (currentDay == Calendar.SUNDAY) 0 else 8 - currentDay
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (topPlayer != null) {
            Text(
                text = buildAnnotatedString {
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Black)) {
                        append(if (topPlayer.anonymousName == currentAnonName) "You" else topPlayer.anonymousName)
                    }
                    append(" Is Leading The Weekly League")
                },
                color = textColor, fontSize = 15.sp, textAlign = TextAlign.Center
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Surface(color = Color(0xFF0284C7).copy(alpha = 0.1f), shape = RoundedCornerShape(50)) {
            Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Timer, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (daysUntilReset == 0) "Resets Tonight at Midnight!" else "League Resets in $daysUntilReset Days",
                    color = Color(0xFF0284C7), fontWeight = FontWeight.Bold, fontSize = 12.sp
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        
        //Row to hold both Coins and Shields side-by-side
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Surface(color = Color(0xFF10B981).copy(alpha = 0.1f), shape = RoundedCornerShape(50)) {
                Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("💰", fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("$spendableCoins", color = Color(0xFF10B981), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
            Surface(color = Color(0xFF06B6D4).copy(alpha = 0.1f), shape = RoundedCornerShape(50)) {
                Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Shield, contentDescription = null, tint = Color(0xFF06B6D4), modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("x$shieldCount", color = Color(0xFF06B6D4), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun LeaderboardListHeader(containerColor: Color, subTextColor: Color) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)).background(containerColor)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Name", color = subTextColor, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                Text("Place", color = subTextColor, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.width(50.dp), textAlign = TextAlign.Center)
                Text("Points", color = subTextColor, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.width(60.dp), textAlign = TextAlign.End)
            }
            HorizontalDivider(color = subTextColor.copy(alpha = 0.2f), thickness = 1.dp, modifier = Modifier.padding(horizontal = 20.dp))
        }
    }
}

@Composable
private fun TierDivider(tier: String, containerColor: Color) {
    Text(
        text = tier,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).background(containerColor).padding(start = 20.dp, top = 16.dp, bottom = 4.dp),
        fontWeight = FontWeight.Black,
        color = when (tier) {
            "Platinum Legend" -> Color(0xFF34D399)
            "Gold Master"     -> Color(0xFFF59E0B)
            "Silver Guardian" -> Color(0xFF94A3B8)
            else              -> Color(0xFFD97706)
        },
        fontSize = 13.sp
    )
}

@Composable
private fun LeaderboardPlayerRow(
    player: LeaderboardEntry, rank: Int, isMe: Boolean, listContainerColor: Color,
    textColor: Color, subTextColor: Color, spendableCoins: Int, onCheerClicked: (String, String) -> Unit,
    onBroke: () -> Unit
) {
    val rowBg = if (isMe) Color(0xFF0284C7).copy(alpha = 0.15f) else Color.Transparent
    var hasCheered by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).background(listContainerColor)) {
        Row(
            modifier = Modifier.fillMaxWidth().background(rowBg).padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).background(if (isMe) Color(0xFF0284C7) else Color.Gray.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = Color.White)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (isMe) "${player.anonymousName} (You)" else player.anonymousName,
                        fontWeight = if (isMe) FontWeight.Bold else FontWeight.SemiBold,
                        color = if (isMe) Color(0xFF0284C7) else textColor,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )

                    if (!isMe) {
                        InteractiveCheerButton(
                            hasCheered = hasCheered,
                            spendableCoins = spendableCoins,
                            onBroke = onBroke,
                            onCheer = {
                                hasCheered = true
                                onCheerClicked(player.userId, player.anonymousName)
                            }
                        )
                    } else {
                        Text("🔥 ${player.cheers} Cheers Received", fontSize = 11.sp, color = subTextColor)
                    }
                }
            }
            Text("$rank", fontWeight = FontWeight.Bold, color = textColor, modifier = Modifier.width(50.dp), textAlign = TextAlign.Center)
            Text("${player.xp}", fontWeight = FontWeight.Black, color = textColor, modifier = Modifier.width(60.dp), textAlign = TextAlign.End)
        }
    }
}

@Composable
private fun InteractiveCheerButton(
    hasCheered: Boolean, 
    spendableCoins: Int, 
    onBroke: () -> Unit,
    onCheer: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    // 1. The Button Bounce State
    val cheerScale = remember { Animatable(1f) }

    // 2. The Lottie State
    val lottieComposition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.confetti))
    var isPlayingLottie by remember { mutableStateOf(false) }

    //drives the animation forward when isPlayingLottie becomes true
    val lottieProgress by animateLottieCompositionAsState(
        composition = lottieComposition,
        isPlaying = isPlayingLottie,
        iterations = 1,
        speed = 1.2f //Speeds up the explosion slightly for better game-feel
    )

    // Reset the Lottie state when it finishes so it doesn't get stuck
    LaunchedEffect(lottieProgress) {
        if (lottieProgress == 1f) {
            isPlayingLottie = false
        }
    }

    //contentAlignment = Center so the explosion happens right over the button
    Box(contentAlignment = Alignment.Center) {

        //  LAYER 1: THE BUTTON (Bottom Layer)
        TextButton(
            onClick = {
                if (!hasCheered) {
                    if (spendableCoins >= 10) {
                        onCheer()

                        // 1. Vibrate
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                        // 2. Play Sound Effect
                        try {
                            val mediaPlayer = MediaPlayer.create(context, R.raw.cheer_sound)
                            mediaPlayer.start()
                            mediaPlayer.setOnCompletionListener { it.release() }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }

                        // 3. Trigger Lottie Explosion
                        isPlayingLottie = true

                        // 4. Bounce the Button
                        coroutineScope.launch {
                            cheerScale.animateTo(1.4f, animationSpec = tween(100))
                            cheerScale.animateTo(1f, animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            ))
                        }
                    } else {
                        // custom Compose popup instead of the Toast
                        onBroke()
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                }
            },
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier.height(24.dp)
        ) {
            Text(
                text = if (hasCheered) "❤️ Cheered!" else "👏 -10 coins",
                fontSize = 11.sp,
                fontWeight = if (hasCheered) FontWeight.Black else FontWeight.Normal,
                color = if (hasCheered) Color(0xFF10B981) else if (spendableCoins < 10) Color.Gray else Color(0xFF0284C7), // Grey out if broke
                modifier = Modifier.scale(cheerScale.value)
            )
        }

        //  LAYER 2: THE LOTTIE EXPLOSION (Top Layer)
        if (isPlayingLottie || lottieProgress in 0.01f..0.99f) {
            LottieAnimation(
                composition = lottieComposition,
                progress = { lottieProgress },
                modifier = Modifier
                    .requiredSize(150.dp)
                    .zIndex(10f)
            )
        }
    }
}

@Composable
private fun LeaderboardListFooter(containerColor: Color) {
    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)).background(containerColor).height(24.dp))
}

@Composable
fun PodiumCard(player: LeaderboardEntry, rankText: String, height: androidx.compose.ui.unit.Dp, cardColor: Color, pillColor: Color, textColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(100.dp)) {
        Box(modifier = Modifier.size(64.dp).clip(CircleShape).background(Color.Gray.copy(alpha = 0.3f)), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(40.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = player.anonymousName, fontWeight = FontWeight.Bold, color = textColor, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(modifier = Modifier.height(12.dp))
        Card(modifier = Modifier.fillMaxWidth().height(height), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = cardColor), elevation = CardDefaults.cardElevation(4.dp)) {
            Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text(rankText, fontWeight = FontWeight.Black, fontSize = 20.sp, color = Color(0xFF451A03))
                Spacer(modifier = Modifier.height(8.dp))
                Box(modifier = Modifier.background(pillColor.copy(alpha = 0.3f), RoundedCornerShape(50)).padding(horizontal = 12.dp, vertical = 6.dp), contentAlignment = Alignment.Center) {
                    Text("${player.xp} XP", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF451A03))
                }
            }
        }
    }
}
