package com.finadapt.adaptivefinance.feature.community

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
    // 🟢 DYNAMIC THEME COLORS
    val bgColor = if (isDarkMode) Color(0xFF0F172A) else Color(0xFFF8FAFC)
    val cardBg = if (isDarkMode) Color(0xFF1E293B) else Color.White
    val textColor = if (isDarkMode) Color(0xFFF1F5F9) else Color(0xFF0F172A)
    val subTextColor = if (isDarkMode) Color(0xFF94A3B8) else Color.Gray

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor),
        contentPadding = PaddingValues(top = 48.dp, start = 20.dp, end = 20.dp, bottom = 100.dp)
    ) {
        item {
            Text("Community", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = textColor)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Anonymous Global Leaderboard. Only XP is shared.", color = subTextColor, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(24.dp))
        }

        if (isLoading) {
            item {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF0284C7))
                }
            }
        } else if (leaderboardData.isEmpty()) {
            item {
                Text("Leaderboard is empty or offline.", color = subTextColor, modifier = Modifier.padding(16.dp))
            }
        } else {
            itemsIndexed(leaderboardData) { index, player ->
                val rank = index + 1
                val isMe = player.anonymousName == currentAnonName

                // Top 3 Colors
                val rankColor = when (rank) {
                    1 -> Color(0xFFF59E0B) // Gold
                    2 -> Color(0xFF94A3B8) // Silver
                    3 -> Color(0xFFB45309) // Bronze
                    else -> subTextColor
                }

                // Highlight my row
                val rowBg = if (isMe) Color(0xFF0284C7).copy(alpha = 0.1f) else cardBg
                val outlineColor = if (isMe) Color(0xFF0284C7) else Color.Transparent

                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = rowBg),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(if (isMe) 1.dp else 0.dp, outlineColor),
                    elevation = CardDefaults.cardElevation(if (isMe || !isDarkMode) 2.dp else 0.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Rank Number / Trophy
                        Box(modifier = Modifier.width(32.dp), contentAlignment = Alignment.Center) {
                            if (rank <= 3) {
                                Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = rankColor)
                            } else {
                                Text("#$rank", fontWeight = FontWeight.Bold, color = rankColor)
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // Avatar Bubble
                        Box(modifier = Modifier.size(40.dp).background(if (isMe) Color(0xFF0284C7) else Color(0xFFE2E8F0), CircleShape), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = if (isMe) Color.White else Color.Gray)
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // Name & Tier
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isMe) "${player.anonymousName} (You)" else player.anonymousName,
                                fontWeight = FontWeight.Bold,
                                color = if (isMe) Color(0xFF0284C7) else textColor
                            )
                            Text(player.tier, color = subTextColor, fontSize = 12.sp)
                        }

                        // XP Score
                        Text("${player.xp} XP", fontWeight = FontWeight.Black, color = if (isMe) Color(0xFF0284C7) else textColor)
                    }
                }
            }
        }
    }
}