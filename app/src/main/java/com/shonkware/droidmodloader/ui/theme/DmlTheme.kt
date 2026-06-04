package com.shonkware.droidmodloader.ui.theme

import android.app.Activity
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

object DmlColors {
    val Background = Color(0xFF07080A)
    val BackgroundRaised = Color(0xFF0D0F13)

    val Surface = Color(0xFF12151B)
    val SurfaceRaised = Color(0xFF181C23)
    val SurfacePanel = Color(0xFF20242D)

    val BorderDim = Color(0xFF343A46)
    val BorderHot = Color(0xFF8E2724)

    val Red = Color(0xFFE0413A)
    val RedSoft = Color(0xFFB73531)
    val RedDark = Color(0xFF5A1718)

    val Amber = Color(0xFFE0A13A)
    val Green = Color(0xFF6EBB75)

    val Text = Color(0xFFEDE6D8)
    val TextMuted = Color(0xFFB8B0A2)
    val TextDim = Color(0xFF827B70)
}

private val DmlColorScheme = darkColorScheme(
    primary = DmlColors.Red,
    onPrimary = Color.White,

    secondary = DmlColors.Amber,
    onSecondary = Color(0xFF1A1204),

    tertiary = DmlColors.Green,
    onTertiary = Color(0xFF061107),

    background = DmlColors.Background,
    onBackground = DmlColors.Text,

    surface = DmlColors.Surface,
    onSurface = DmlColors.Text,

    surfaceVariant = DmlColors.SurfaceRaised,
    onSurfaceVariant = DmlColors.TextMuted,

    outline = DmlColors.BorderHot,
    outlineVariant = DmlColors.BorderDim,

    error = Color(0xFFFF6B64),
    onError = Color(0xFF2A0303)
)

private val DmlShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun DmlTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window

            window.statusBarColor = DmlColors.Background.toArgb()
            window.navigationBarColor = DmlColors.Background.toArgb()

            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = DmlColorScheme,
        typography = MaterialTheme.typography,
        shapes = DmlShapes,
        content = content
    )
}

object DmlDefaults {
    @Composable
    fun panelCardColors() = CardDefaults.cardColors(
        containerColor = DmlColors.Surface,
        contentColor = DmlColors.Text
    )

    @Composable
    fun raisedCardColors() = CardDefaults.cardColors(
        containerColor = DmlColors.SurfaceRaised,
        contentColor = DmlColors.Text
    )
}