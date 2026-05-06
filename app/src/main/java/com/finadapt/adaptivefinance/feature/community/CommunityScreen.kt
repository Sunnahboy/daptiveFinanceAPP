package com.finadapt.adaptivefinance.feature.community

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.finadapt.adaptivefinance.data.remote.LeaderboardEntry

@Composable
fun CommunityScreen(
    leaderboardData: List<LeaderboardEntry>,
    currentAnonName: String,
    isLoading: Boolean,
    isDarkMode: Boolean = false
) {
    //PREMIUM ADAPTIVE COLORS
    val bgColor = if (isDarkMode) Color(0xFF0F172A) else Color(0xFFE2E8F0)
    val listContainerColor = if (isDarkMode) Color(0xFF1E293B).copy(alpha = 0.8f) else Color.White.copy(alpha = 0.6f)
    val textColor = if (isDarkMode) Color.White else Color(0xFF0F172A)
    val subTextColor = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B)

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize().background(bgColor), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFF0284C7))
        }
    } else if (leaderboardData.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().background(bgColor), contentAlignment = Alignment.Center) {
            Text("Leaderboard is empty or offline.", color = subTextColor)
        }
    } else {
        val top3 = leaderboardData.take(3)
        val topPlayer = leaderboardData.firstOrNull()

        // One unified LazyColumn so the entire screen scrolls flawlessly
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(bgColor),
            contentPadding = PaddingValues(top = 48.dp, bottom = 120.dp) // Large bottom padding clears the Nav Bar!
        ) {
            // --- 1. THE PODIUM SECTION ---
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    // 2nd Place
                    if (top3.size > 1) {
                        PodiumCard(top3[1], "2nd", 130.dp, Color(0xFFCBD5E1), Color(0xFF94A3B8), textColor)
                    }
                    // 1st Place
                    if (top3.isNotEmpty()) {
                        PodiumCard(top3[0], "1st", 170.dp, Color(0xFFFDE047), Color(0xFFEAB308), textColor)
                    }
                    // 3rd Place
                    if (top3.size > 2) {
                        PodiumCard(top3[2], "3rd", 100.dp, Color(0xFFFDBA74), Color(0xFFD97706), textColor)
                    }
                }
            }

            // --- 2. THE WINNER BANNER ---
            if (topPlayer != null) {
                item {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(style = SpanStyle(fontWeight = FontWeight.Black)) {
                                append(if (topPlayer.anonymousName == currentAnonName) "You" else topPlayer.anonymousName)
                            }
                            append(" Is On Top Of The Leaderboard This Month")
                        },
                        color = textColor,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp)
                    )
                }
            }

            // --- 3. LIST HEADER (Top rounded part of the glass card) ---
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        .background(listContainerColor)
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Name", color = subTextColor, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                            Text("Place", color = subTextColor, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.width(50.dp), textAlign = TextAlign.Center)
                            Text("Points", color = subTextColor, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.width(60.dp), textAlign = TextAlign.End)
                        }
                        HorizontalDivider(color = subTextColor.copy(alpha = 0.2f), thickness = 1.dp, modifier = Modifier.padding(horizontal = 20.dp))
                    }
                }
            }

            // --- 4. THE SCROLLING PLAYERS (Middle of the glass card) ---
            itemsIndexed(leaderboardData) { index, player ->
                val rank = index + 1
                val isMe = player.anonymousName == currentAnonName

                // Highlight user's row gently
                val rowBg = if (isMe) Color(0xFF0284C7).copy(alpha = 0.15f) else Color.Transparent

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .background(listContainerColor) // Solid sides to continue the card illusion
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(rowBg)
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Avatar + Name
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(if (isMe) Color(0xFF0284C7) else Color.Gray.copy(alpha = 0.3f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = Color.White)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = if (isMe) "${player.anonymousName} (You)" else player.anonymousName,
                                fontWeight = if (isMe) FontWeight.Bold else FontWeight.SemiBold,
                                color = if (isMe) Color(0xFF0284C7) else textColor,
                                maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Place
                        Text(
                            text = "$rank",
                            fontWeight = FontWeight.Bold,
                            color = textColor,
                            modifier = Modifier.width(50.dp),
                            textAlign = TextAlign.Center
                        )

                        // Points
                        Text(
                            text = "${player.xp}",
                            fontWeight = FontWeight.Black,
                            color = textColor,
                            modifier = Modifier.width(60.dp),
                            textAlign = TextAlign.End
                        )
                    }
                }
            }

            // --- 5. LIST FOOTER (Bottom rounded part of the glass card) ---
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                        .background(listContainerColor)
                        .height(24.dp) // Just enough padding to close the card beautifully
                )
            }
        }
    }
}

@Composable
fun PodiumCard(
    player: LeaderboardEntry,
    rankText: String,
    height: androidx.compose.ui.unit.Dp,
    cardColor: Color,
    pillColor: Color,
    textColor: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(100.dp)
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(Color.Gray.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(40.dp))
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Name
        Text(
            text = player.anonymousName,
            fontWeight = FontWeight.Bold,
            color = textColor,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(12.dp))

        // The Podium Block
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(height),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = cardColor),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = rankText,
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    color = Color(0xFF451A03) // Dark text to contrast the bright podium blocks
                )
                Spacer(modifier = Modifier.height(8.dp))

                // XP Pill
                Box(
                    modifier = Modifier
                        .background(pillColor.copy(alpha = 0.3f), RoundedCornerShape(50))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${player.xp} XP",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color(0xFF451A03)
                    )
                }
            }
        }
    }
}