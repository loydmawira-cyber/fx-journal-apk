package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CandlestickChart
import androidx.compose.material.icons.filled.DynamicFeed
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AppNavScreen
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.OnElectricCyan

@Composable
fun FxBottomBar(
    currentScreen: AppNavScreen,
    onTabSelected: (AppNavScreen) -> Unit,
    onPlusClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.98f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tab 1: Feed
            BottomNavItem(
                icon = Icons.Default.DynamicFeed,
                label = "Feed",
                isSelected = currentScreen == AppNavScreen.FEED,
                onClick = { onTabSelected(AppNavScreen.FEED) },
                testTag = "bottom_tab_feed"
            )

            // Tab 2: Journal
            BottomNavItem(
                icon = Icons.Default.CandlestickChart,
                label = "Journal",
                isSelected = currentScreen == AppNavScreen.JOURNAL,
                onClick = { onTabSelected(AppNavScreen.JOURNAL) },
                testTag = "bottom_tab_journal"
            )

            // Center Spacer for the elevated button
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .offset(y = (-14).dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .shadow(
                            elevation = 8.dp,
                            shape = CircleShape,
                            ambientColor = ElectricCyan,
                            spotColor = ElectricCyan
                        )
                        .clip(CircleShape)
                        .background(ElectricCyan)
                        .clickable { onPlusClick() }
                        .testTag("center_log_trade_fab"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Log Trade",
                        tint = OnElectricCyan,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Tab 4: Traders
            BottomNavItem(
                icon = Icons.Default.Group,
                label = "Traders",
                isSelected = currentScreen == AppNavScreen.TRADERS,
                onClick = { onTabSelected(AppNavScreen.TRADERS) },
                testTag = "bottom_tab_traders"
            )

            // Tab 5: Economic Calendar
            BottomNavItem(
                icon = Icons.Default.CalendarMonth,
                label = "Calendar",
                isSelected = currentScreen == AppNavScreen.BREAKDOWN,
                onClick = { onTabSelected(AppNavScreen.BREAKDOWN) },
                testTag = "bottom_tab_calendar"
            )
        }
    }
}

@Composable
private fun BottomNavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Column(
        modifier = Modifier
            .size(56.dp)
            .clickable { onClick() }
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) ElectricCyan else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 11.sp
            ),
            color = if (isSelected) ElectricCyan else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
