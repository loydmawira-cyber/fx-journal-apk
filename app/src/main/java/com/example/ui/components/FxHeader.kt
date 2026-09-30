package com.example.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldProfit
import com.example.ui.theme.ThemeMode

@Composable
fun FxHeader(
    title: String = "FX Journal",
    subtitle: String? = "FEED",
    showBack: Boolean = false,
    onBackClick: () -> Unit = {},
    themeMode: ThemeMode = ThemeMode.AMOLED,
    onThemeClick: () -> Unit = {},
    onPaletteClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    profileName: String = "Trader",
    profileHandle: String? = null
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.98f))
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showBack) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("header_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
        }

        // FX Journal Logo Badge (as requested: "Show the new FX Journal logo in the header")
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .border(
                    width = 1.dp,
                    color = ElectricCyan.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(8.dp)
                )
                .clickable { onProfileClick() },
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_fx_logo),
                contentDescription = "FX Journal Logo",
                modifier = Modifier.size(26.dp),
                contentScale = ContentScale.Fit
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // App Branding and current screen
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "FX JOURNAL",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    letterSpacing = 0.4.sp
                ),
                color = ElectricCyan
            )
        }

        // Live Pill Indicator
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(EmeraldProfit)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = "LIVE",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.width(4.dp))

        // Theme Mode Toggle Button
        Box(
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                onClick = onThemeClick,
                modifier = Modifier
                    .size(38.dp)
                    .testTag("theme_toggle_button")
            ) {
                Icon(
                    imageVector = if (themeMode == ThemeMode.LIGHT) Icons.Default.LightMode else Icons.Default.DarkMode,
                    contentDescription = "Theme Mode",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
            // Active Theme Status Dot
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 6.dp, end = 6.dp)
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(EmeraldProfit)
            )
        }

        // Accent Color Palette Button
        IconButton(
            onClick = onPaletteClick,
            modifier = Modifier
                .size(38.dp)
                .testTag("palette_button")
        ) {
            Icon(
                imageVector = Icons.Default.Palette,
                contentDescription = "App Colors",
                tint = ElectricCyan,
                modifier = Modifier.size(20.dp)
            )
        }

        // Notification Button with Ping Dot
        Box(
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                onClick = onNotificationsClick,
                modifier = Modifier
                    .size(38.dp)
                    .testTag("notifications_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "Notifications",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 8.dp, end = 8.dp)
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(ElectricCyan)
            )
        }

        // Trader Profile Avatar
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .clickable { onProfileClick() },
            contentAlignment = Alignment.Center
        ) {
            AvatarContent(
                handle = profileHandle,
                initials = profileName.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("").uppercase().ifBlank { "TR" },
                textStyle = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                ),
                textColor = ElectricCyan
            )
            // Online Green Pip
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(EmeraldProfit)
                    .border(1.dp, MaterialTheme.colorScheme.surface, CircleShape)
            )
        }
    }
}
