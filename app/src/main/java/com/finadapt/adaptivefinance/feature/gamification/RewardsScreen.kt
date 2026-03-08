package com.finadapt.adaptivefinance.feature.gamification
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
    onBuyShield: () -> Unit,
) {

    val badgeRows = badges.chunked(2) //split the badges into list of two for the grid

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color((0xFFF8FAFC))),
        contentPadding = PaddingValues(top = 48.dp, start = 20.dp, end = 20.dp, bottom = 100.dp)
    ) {
        // -- header--
        item {
            Text(
                "Rewards & Trophies",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Complete AI challenges to earn XP and unlock badges.",
                color = Color.Gray,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(24.dp))

        }

        // The XP Economy (Streak shield Store)
        item {
            Card(
                modifier = Modifier.fillMaxSize(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF06B6D4)),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(8.dp)

            ) {
                Row(
                    modifier = Modifier.padding(20.dp).fillMaxSize(),
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
                            Spacer(modifier = Modifier.height(8.dp))
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
                            fontSize = 12.sp
                        )

                    }

                    //The Buy Button (disabled if they don't have enough XP)
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
                Spacer(modifier = Modifier.height(32.dp))
            }
        }

            // Safe Grid Implementation inside lazyColumn
            items(badgeRows) { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    for (badge in rowItems) {
                        BadgeCard(badge = badge, modifier = Modifier.weight(1f))
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
fun BadgeCard(badge: Badge, modifier: Modifier = Modifier) {
    // If it's locked, we dim it out and make it grayscale
    val alpha = if (badge.isUnlocked) 1f else 0.4f
    val bgColor = if (badge.isUnlocked) Color.White else Color(0xFFF1F5F9)

    Card(
        modifier = modifier.height(160.dp).alpha(alpha),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(if (badge.isUnlocked) 4.dp else 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (!badge.isUnlocked) {
                Icon(Icons.Default.Lock, contentDescription = "Locked", tint = Color.Gray, modifier = Modifier.size(16.dp).align(Alignment.End))
            } else {
                Spacer(modifier = Modifier.height(16.dp)) // Balance the layout
            }

            // The Badge Icon
            Box(
                modifier = Modifier.size(56.dp).background(Color(0xFFF8FAFC), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(badge.icon, fontSize = 28.sp)
            }
            Spacer(modifier = Modifier.height(12.dp))

            Text(badge.title, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), fontSize = 14.sp, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(4.dp))
            Text(badge.description, color = Color.Gray, fontSize = 11.sp, textAlign = TextAlign.Center, lineHeight = 14.sp)
        }
    }
}

