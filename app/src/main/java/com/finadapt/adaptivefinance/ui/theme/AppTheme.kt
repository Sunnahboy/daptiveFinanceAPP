package com.finadapt.adaptivefinance.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

// Emerald
val EmeraldLight = lightColorScheme(
    primary = Color(0xFF0284C7),
    onPrimary = Color.White,
    secondary = Color(0xFF10B981),
    background = Color(0xFFF8FAFC),
    surface = Color.White,
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF0F172A),
    onSurfaceVariant = Color.Gray,
    surfaceVariant = Color(0xFFF1F5F9),
    error = Color(0xFFEF4444)
)

val EmeraldDark = darkColorScheme(
    primary = Color(0xFF38BDF8),
    onPrimary = Color(0xFF0F172A),
    secondary = Color(0xFF34D399),
    background = Color(0xFF0F172A),
    surface = Color(0xFF1E293B),
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFF94A3B8),
    surfaceVariant = Color(0xFF334155),
    error = Color(0xFFF87171)
)

// Ocean
val OceanLight = lightColorScheme(
    primary = Color(0xFF1E3A8A),
    onPrimary = Color.White,
    secondary = Color(0xFF3B82F6),
    background = Color(0xFFF0F9FF),
    surface = Color.White,
    onBackground = Color(0xFF082F49),
    onSurface = Color(0xFF082F49),
    onSurfaceVariant = Color(0xFF475569),
    surfaceVariant = Color(0xFFE0F2FE),
    error = Color(0xFFEF4444)
)

val OceanDark = darkColorScheme(
    primary = Color(0xFF60A5FA),
    onPrimary = Color(0xFF0F172A),
    secondary = Color(0xFF3B82F6),
    background = Color(0xFF082F49),
    surface = Color(0xFF0C4A6E),
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFF94A3B8),
    surfaceVariant = Color(0xFF075985),
    error = Color(0xFFF87171)
)

// Sunset
val SunsetLight = lightColorScheme(
    primary = Color(0xFFC2410C),
    onPrimary = Color.White,
    secondary = Color(0xFFF59E0B),
    background = Color(0xFFFFF7ED),
    surface = Color.White,
    onBackground = Color(0xFF431407),
    onSurface = Color(0xFF431407),
    onSurfaceVariant = Color(0xFF78350F),
    surfaceVariant = Color(0xFFFFEDD5),
    error = Color(0xFFEF4444)
)

val SunsetDark = darkColorScheme(
    primary = Color(0xFFFB923C),
    onPrimary = Color(0xFF431407),
    secondary = Color(0xFFFBBF24),
    background = Color(0xFF431407),
    surface = Color(0xFF78350F),
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFFFDBA74),
    surfaceVariant = Color(0xFF9A3412),
    error = Color(0xFFF87171)
)

// Forest
val ForestLight = lightColorScheme(
    primary = Color(0xFF064E3B),
    onPrimary = Color.White,
    secondary = Color(0xFF10B981),
    background = Color(0xFFF0FDF4),
    surface = Color.White,
    onBackground = Color(0xFF022C22),
    onSurface = Color(0xFF022C22),
    onSurfaceVariant = Color(0xFF065F46),
    surfaceVariant = Color(0xFFDCFCE7),
    error = Color(0xFFEF4444)
)

val ForestDark = darkColorScheme(
    primary = Color(0xFF34D399),
    onPrimary = Color(0xFF022C22),
    secondary = Color(0xFF10B981),
    background = Color(0xFF022C22),
    surface = Color(0xFF064E3B),
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFF6EE7B7),
    surfaceVariant = Color(0xFF065F46),
    error = Color(0xFFF87171)
)

// Royal
val RoyalLight = lightColorScheme(
    primary = Color(0xFF4C1D95),
    onPrimary = Color.White,
    secondary = Color(0xFF8B5CF6),
    background = Color(0xFFF5F3FF),
    surface = Color.White,
    onBackground = Color(0xFF2E1065),
    onSurface = Color(0xFF2E1065),
    onSurfaceVariant = Color(0xFF5B21B6),
    surfaceVariant = Color(0xFFEDE9FE),
    error = Color(0xFFEF4444)
)

val RoyalDark = darkColorScheme(
    primary = Color(0xFFA78BFA),
    onPrimary = Color(0xFF2E1065),
    secondary = Color(0xFF8B5CF6),
    background = Color(0xFF2E1065),
    surface = Color(0xFF4C1D95),
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFFC4B5FD),
    surfaceVariant = Color(0xFF5B21B6),
    error = Color(0xFFF87171)
)

enum class AppTheme(val title: String, val lightColorScheme: ColorScheme, val darkColorScheme: ColorScheme) {
    EMERALD("Emerald Blue", EmeraldLight, EmeraldDark),
    OCEAN("Deep Ocean", OceanLight, OceanDark),
    SUNSET("Sunset Orange", SunsetLight, SunsetDark),
    FOREST("Lush Forest", ForestLight, ForestDark),
    ROYAL("Royal Purple", RoyalLight, RoyalDark);

    companion object {
        fun fromString(name: String?): AppTheme {
            return entries.find { it.name == name } ?: EMERALD
        }
    }
}

val LocalAppTheme = compositionLocalOf { AppTheme.EMERALD }