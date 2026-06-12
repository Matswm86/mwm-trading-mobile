package no.mwmai.backtest.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val DarkColors = darkColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    secondary = Accent,
    onSecondary = OnPrimary,
    background = Background,
    onBackground = OnBackground,
    surface = Surface,
    onSurface = OnBackground,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = Muted,
    outline = OutlineSoft,
    outlineVariant = OutlineSoft,
    error = Negative,
)

// Slightly tighter, more confident type ramp than the Material default —
// big numerals on cards, airy uppercase labels for section headers.
private val AppTypography = Typography().let { t ->
    t.copy(
        headlineMedium = t.headlineMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
        titleLarge = t.titleLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.25).sp),
        titleMedium = t.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        labelMedium = TextStyle(
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.2.sp,
        ),
        labelSmall = TextStyle(
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.8.sp,
        ),
    )
}

@Composable
fun MwmBacktestTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    // Dark-only: the dashboard is a glanceable instrument panel.
    MaterialTheme(
        colorScheme = DarkColors,
        typography = AppTypography,
        content = content,
    )
}
