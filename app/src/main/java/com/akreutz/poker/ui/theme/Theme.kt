package com.akreutz.poker.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Felt80,
    onPrimary = Felt20,
    primaryContainer = Felt30,
    onPrimaryContainer = Felt90,
    inversePrimary = Felt40,

    secondary = FeltGrey80,
    onSecondary = FeltGrey20,
    secondaryContainer = FeltGrey30,
    onSecondaryContainer = FeltGrey90,

    tertiary = Gold80,
    onTertiary = Gold20,
    tertiaryContainer = Gold30,
    onTertiaryContainer = Gold90,

    error = Red80,
    onError = Red20,
    errorContainer = Red30,
    onErrorContainer = Red90,

    background = Neutral10,
    onBackground = Neutral90,
    surface = Neutral10,
    onSurface = Neutral90,
    surfaceVariant = NeutralVariant30,
    onSurfaceVariant = NeutralVariant80,
    surfaceTint = Felt80,
    inverseSurface = Neutral90,
    inverseOnSurface = Neutral20,
    outline = NeutralVariant60,
    outlineVariant = NeutralVariant30,
    scrim = Neutral0,
)

private val LightColorScheme = lightColorScheme(
    primary = Felt40,
    onPrimary = Felt100,
    primaryContainer = Felt90,
    onPrimaryContainer = Felt10,
    inversePrimary = Felt80,

    secondary = FeltGrey40,
    onSecondary = FeltGrey100,
    secondaryContainer = FeltGrey90,
    onSecondaryContainer = FeltGrey10,

    tertiary = Gold40,
    onTertiary = Gold100,
    tertiaryContainer = Gold90,
    onTertiaryContainer = Gold10,

    error = Red40,
    onError = Red100,
    errorContainer = Red90,
    onErrorContainer = Red10,

    background = Neutral99,
    onBackground = Neutral10,
    surface = Neutral99,
    onSurface = Neutral10,
    surfaceVariant = NeutralVariant90,
    onSurfaceVariant = NeutralVariant30,
    surfaceTint = Felt40,
    inverseSurface = Neutral20,
    inverseOnSurface = Neutral95,
    outline = NeutralVariant50,
    outlineVariant = NeutralVariant80,
    scrim = Neutral0,
)

@Composable
fun PokerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+, but disabled by default so the app's
    // felt-green brand color is used instead of a wallpaper-derived palette.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}