package com.finadapt.adaptivefinance.feature.gamification

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class Badge(
    val id: String,
    val title: String,
    val description: String,
    val icon: String,
    val isUnlocked: Boolean,
)

@Composable
fun RewardsScreen(
    userXp: Int,
    shieldCount: Int,
    badges: List<Badge>,
    isDarkMode: Boolean = false, // 🟢 NEW: Dark mode state passed in
    onBuyShield: () -> Unit,
) {
    // 🟢 DYNAMIC THEME COLORS
    val bgColor = if (isDarkMode) Color(0xFF0F172A) else Color(0xFFF8FAFC)
    val textColor = if (isDarkMode) Color(0xFFF1F5F9) else Color(0xFF0F172A)
    val subTextColor = if (isDarkMode) Color(0xFF94A3B8) else Color.Gray

    val badgeRows = badges.chunked(2) // split the badges into list of two for the grid

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor), // 🟢 Dynamic background
        contentPadding = PaddingValues(top = 48.dp, start = 20.dp, end = 20.dp, bottom = 100.dp)
    ) {
        // -- HEADER --
        item {
            Text(
                "Rewards & Trophies",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = textColor // 🟢 Dynamic
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Complete AI challenges to earn XP and unlock badges.",
                color = subTextColor, // 🟢 Dynamic
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        // -- THE XP ECONOMY (Streak Shield Store) --
        item {
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp), // Added padding here instead of Spacer inside
                colors = CardDefaults.cardColors(containerColor = Color(0xFF06B6D4)), // Cyan stays vibrant in both modes!
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(if (isDarkMode) 0.dp else 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(20.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Shield,
                                contentDescription = "Shield",
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "STREAK SHIELD",
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                letterSpacing = 1.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "You have $shieldCount active",
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Protects your streak if you miss a day of logging.",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // The Buy Button (disabled if they don't have enough XP)
                    val shieldCost = 500
                    val canAfford = userXp >= shieldCost
                    Button(
                        onClick = onBuyShield,
                        enabled = canAfford,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            disabledContainerColor = Color.White.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (canAfford) "-$shieldCost XP" else "Need $shieldCost XP",
                            color = if (canAfford) Color(0xFF06B6D4) else Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // -- SAFE GRID IMPLEMENTATION --
        items(badgeRows) { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                for (badge in rowItems) {
                    BadgeCard(
                        badge = badge,
                        isDarkMode = isDarkMode, // 🟢 Pass theme state down
                        modifier = Modifier.weight(1f)
                    )
                }
                // If there's an odd number of badges, fill the empty space
                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun BadgeCard(badge: Badge, isDarkMode: Boolean, modifier: Modifier = Modifier) {
    // 🟢 DYNAMIC BADGE COLORS
    val alpha = if (badge.isUnlocked) 1f else 0.4f

    // Unlocked = White/Slate. Locked = Gray/Dark Slate.
    val cardBgColor = if (isDarkMode) {
        if (badge.isUnlocked) Color(0xFF1E293B) else Color(0xFF0F172A)
    } else {
        if (badge.isUnlocked) Color.White else Color(0xFFF1F5F9)
    }

    val textColor = if (isDarkMode) Color(0xFFF1F5F9) else Color(0xFF0F172A)
    val subTextColor = if (isDarkMode) Color(0xFF94A3B8) else Color.Gray
    val iconBgColor = if (isDarkMode) Color(0xFF334155) else Color(0xFFF8FAFC)

    Card(
        modifier = modifier.height(160.dp).alpha(alpha),
        colors = CardDefaults.cardColors(containerColor = cardBgColor),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(if (badge.isUnlocked && !isDarkMode) 4.dp else 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Lock Icon Logic
            if (!badge.isUnlocked) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = "Locked",
                    tint = subTextColor,
                    modifier = Modifier.size(16.dp).align(Alignment.End)
                )
            } else {
                Spacer(modifier = Modifier.height(16.dp)) // Balance the layout
            }

            // The Badge Icon
            Box(
                modifier = Modifier.size(56.dp).background(iconBgColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(badge.icon, fontSize = 28.sp)
            }
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = badge.title,
                fontWeight = FontWeight.Bold,
                color = textColor, // 🟢 Dynamic
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = badge.description,
                color = subTextColor, // 🟢 Dynamic
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                lineHeight = 14.sp
            )
        }
    }
}