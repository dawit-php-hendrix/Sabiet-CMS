package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val SabietDarkColorScheme = darkColorScheme(
    primary = SabietAccent,
    onPrimary = SabietOnPrimaryContainer,
    primaryContainer = SabietPrimary,
    onPrimaryContainer = SabietOnPrimary,
    secondary = SabietSecondary,
    onSecondary = SabietOnSecondary,
    background = SabietBackgroundDark,
    onBackground = SabietOnBackgroundDark,
    surface = SabietSurfaceDark,
    onSurface = SabietOnSurfaceDark,
    surfaceVariant = SabietSurfaceVariantDark,
    outline = SabietOutlineDark
)

private val SabietLightColorScheme = lightColorScheme(
    primary = SabietPrimary,
    onPrimary = SabietOnPrimary,
    primaryContainer = SabietPrimaryContainer,
    onPrimaryContainer = SabietOnPrimaryContainer,
    secondary = SabietSecondary,
    onSecondary = SabietOnSecondary,
    background = SabietBackgroundLight,
    onBackground = SabietOnBackgroundLight,
    surface = SabietSurfaceLight,
    onSurface = SabietOnSurfaceLight,
    surfaceVariant = SabietSurfaceVariantLight,
    outline = SabietOutlineLight
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> SabietDarkColorScheme
        else -> SabietLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
