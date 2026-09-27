package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Obsidian Kinetic Base Palette
val ObsidianDark = Color(0xFF0F131C)
val ObsidianLowest = Color(0xFF0A0E16)
val ObsidianLow = Color(0xFF181C24)
val ObsidianSurface = Color(0xFF1C2028)
val ObsidianHigh = Color(0xFF262A33)
val ObsidianHighest = Color(0xFF31353E)
val ObsidianBright = Color(0xFF353942)

// Text & Content Neutrals
val OnSurfaceLight = Color(0xFFDFE2EE)
val OnSurfaceVariantMuted = Color(0xFFBCC9CD)
val OutlineMuted = Color(0xFF869397)
val OutlineVariantMuted = Color(0xFF3D494C)

// Telemetry & Accents
val ElectricCyan = Color(0xFF4CD7F6)
val ElectricCyanContainer = Color(0xFF06B6D4)
val OnElectricCyan = Color(0xFF003640)
val OnElectricCyanContainer = Color(0xFF00424F)

val EmeraldProfit = Color(0xFF4EDEA3)
val EmeraldProfitContainer = Color(0xFF00A572)
val OnEmerald = Color(0xFF003824)

val CrimsonLoss = Color(0xFFFFB4AB)
val CrimsonLossBright = Color(0xFFEF4444)
val CrimsonLossContainer = Color(0xFF93000A)
val OnCrimson = Color(0xFF690005)

val AmberWarning = Color(0xFFF59E0B)
val AmberWarningContainer = Color(0xFF78350F)

// AMOLED Pure Pitch Mode
val AmoledBlack = Color(0xFF000000)
val AmoledSurface = Color(0xFF0D0D11)
val AmoledContainer = Color(0xFF141418)

// Clinical Light Mode
val ClinicalLightBg = Color(0xFFF8FAFC)
val ClinicalLightSurface = Color(0xFFFFFFFF)
val ClinicalLightContainer = Color(0xFFEEF2F6)
val ClinicalLightText = Color(0xFF0F172A)
val ClinicalLightMuted = Color(0xFF64748B)

enum class ThemeMode(val title: String, val subtitle: String) {
    DARK_SLATE("Dark Slate", "Obsidian Tech"),
    AMOLED("AMOLED", "Pure Pitch #000"),
    LIGHT("Light Mode", "Clinical Crisp")
}
