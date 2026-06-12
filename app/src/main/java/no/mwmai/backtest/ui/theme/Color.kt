package no.mwmai.backtest.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Deep-ink fintech palette. Background is near-black with a cold blue cast;
// surfaces step up in two layers so cards read as elevation without shadows.
val Background = Color(0xFF060A13)
val Surface = Color(0xFF0D1422)
val SurfaceVariant = Color(0xFF16203A)
val OutlineSoft = Color(0xFF1F2B47)

val Primary = Color(0xFF2DD4A7)
val OnPrimary = Color(0xFF04261B)
val Accent = Color(0xFF38BDF8)

val Positive = Color(0xFF2DD4A7)
val Negative = Color(0xFFFB7185)
val Warn = Color(0xFFFBBF24)

val OnBackground = Color(0xFFEAF0F8)
val Muted = Color(0xFF7C8AA5)

// Hero / emphasis gradient (emerald → sky), used on the fleet summary card.
val HeroGradient = Brush.linearGradient(
    listOf(Color(0xFF0E3B2E), Color(0xFF0B2C44)),
)
val HeroEdge = Color(0xFF1E4D3D)
