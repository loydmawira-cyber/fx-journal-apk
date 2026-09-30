package com.example.ui.theme

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb

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
private val DefaultAccent = Color(0xFF4CD7F6)
private val DefaultAccentContainer = Color(0xFF06B6D4)
private val DefaultOnAccent = Color(0xFF003640)
private val DefaultOnAccentContainer = Color(0xFF00424F)

/** User-chosen accent color. Saved on the phone and applied across the whole app. */
object AccentPalette {
    private const val PREFS = "fx_journal_ui"
    private const val KEY = "accent_argb"

    private var selected by mutableStateOf<Color?>(null)
    val current: Color? get() = selected

    /** Call once at startup, before the UI is drawn. */
    fun load(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        selected = if (prefs.contains(KEY)) Color(prefs.getInt(KEY, DefaultAccent.toArgb())) else null
    }

    fun set(context: Context, color: Color?) {
        selected = color
        val editor = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
        if (color == null) editor.remove(KEY) else editor.putInt(KEY, color.toArgb())
        editor.apply()
    }

    /** Text/icon color that stays readable on top of the accent. */
    fun contentOn(color: Color): Color =
        if (color.luminance() > 0.45f) Color(0xFF101418) else Color.White

    fun darker(color: Color): Color = Color(
        red = color.red * 0.75f, green = color.green * 0.75f, blue = color.blue * 0.75f, alpha = 1f
    )

    /** A wide range of colors: 12 hues x 4 tones, plus neutrals. */
    val presets: List<Color> by lazy {
        val out = mutableListOf<Color>()
        val tones = listOf(0.55f to 1.0f, 0.75f to 1.0f, 1.0f to 0.95f, 1.0f to 0.6f) // saturation to value
        val hues = listOf(0f, 20f, 40f, 55f, 90f, 140f, 170f, 195f, 215f, 250f, 285f, 325f)
        for ((sat, value) in tones) for (h in hues) out += Color(android.graphics.Color.HSVToColor(floatArrayOf(h, sat, value)))
        out += listOf(
            Color(0xFFFFFFFF), Color(0xFFCBD5E1), Color(0xFF94A3B8), Color(0xFF64748B),
            Color(0xFFF5C518), Color(0xFFFF6B6B), Color(0xFF00E5A8), Color(0xFF7C4DFF)
        )
        out
    }
}

val ElectricCyan: Color get() = AccentPalette.current ?: DefaultAccent
val ElectricCyanContainer: Color get() = AccentPalette.current?.let { AccentPalette.darker(it) } ?: DefaultAccentContainer
val OnElectricCyan: Color get() = AccentPalette.current?.let { AccentPalette.contentOn(it) } ?: DefaultOnAccent
val OnElectricCyanContainer: Color get() = AccentPalette.current?.let { AccentPalette.contentOn(AccentPalette.darker(it)) } ?: DefaultOnAccentContainer

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
