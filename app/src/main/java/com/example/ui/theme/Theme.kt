package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkSlateColorScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = OnElectricCyan,
    primaryContainer = ElectricCyanContainer,
    onPrimaryContainer = OnElectricCyanContainer,
    secondary = EmeraldProfit,
    onSecondary = OnEmerald,
    secondaryContainer = EmeraldProfitContainer,
    background = ObsidianDark,
    onBackground = OnSurfaceLight,
    surface = ObsidianDark,
    onSurface = OnSurfaceLight,
    surfaceVariant = ObsidianHighest,
    onSurfaceVariant = OnSurfaceVariantMuted,
    surfaceContainerLowest = ObsidianLowest,
    surfaceContainerLow = ObsidianLow,
    surfaceContainer = ObsidianSurface,
    surfaceContainerHigh = ObsidianHigh,
    surfaceContainerHighest = ObsidianHighest,
    outline = OutlineMuted,
    outlineVariant = OutlineVariantMuted,
    error = CrimsonLossBright,
    onError = OnCrimson,
    errorContainer = CrimsonLossContainer
)

private val AmoledColorScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = OnElectricCyan,
    primaryContainer = ElectricCyanContainer,
    onPrimaryContainer = OnElectricCyanContainer,
    secondary = EmeraldProfit,
    onSecondary = OnEmerald,
    secondaryContainer = EmeraldProfitContainer,
    background = AmoledBlack,
    onBackground = OnSurfaceLight,
    surface = AmoledBlack,
    onSurface = OnSurfaceLight,
    surfaceVariant = AmoledContainer,
    onSurfaceVariant = OnSurfaceVariantMuted,
    surfaceContainerLowest = AmoledBlack,
    surfaceContainerLow = AmoledSurface,
    surfaceContainer = AmoledContainer,
    surfaceContainerHigh = Color(0xFF1B1B22),
    surfaceContainerHighest = Color(0xFF24242E),
    outline = OutlineMuted,
    outlineVariant = Color(0xFF2A2A36),
    error = CrimsonLossBright,
    onError = OnCrimson,
    errorContainer = CrimsonLossContainer
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0284C7),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F2FE),
    onPrimaryContainer = Color(0xFF0369A1),
    secondary = Color(0xFF059669),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD1FAE5),
    background = ClinicalLightBg,
    onBackground = ClinicalLightText,
    surface = ClinicalLightSurface,
    onSurface = ClinicalLightText,
    surfaceVariant = ClinicalLightContainer,
    onSurfaceVariant = ClinicalLightMuted,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF1F5F9),
    surfaceContainer = ClinicalLightContainer,
    surfaceContainerHigh = Color(0xFFE2E8F0),
    surfaceContainerHighest = Color(0xFFCBD5E1),
    outline = Color(0xFF94A3B8),
    outlineVariant = Color(0xFFCBD5E1),
    error = Color(0xFFDC2626),
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2)
)

@Composable
fun FXJournalTheme(
    themeMode: ThemeMode = ThemeMode.DARK_SLATE,
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeMode) {
        ThemeMode.DARK_SLATE -> DarkSlateColorScheme
        ThemeMode.AMOLED -> AmoledColorScheme
        ThemeMode.LIGHT -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
