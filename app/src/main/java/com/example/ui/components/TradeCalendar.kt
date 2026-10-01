package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Trade
import com.example.ui.theme.CrimsonLossBright
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldProfit
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun TradeCalendar(
    trades: List<Trade>,
    onDateSelected: (Date, List<Trade>) -> Unit
) {
    var calendar by remember { mutableStateOf(Calendar.getInstance()) }
    val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    
    // Group trades by date string (yyyy-MM-dd)
    val tradesByDate = remember(trades) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        trades.groupBy { sdf.format(Date(it.timestamp)) }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        // Calendar Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = monthYearFormat.format(calendar.time),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Row {
                IconButton(onClick = {
                    val newCal = calendar.clone() as Calendar
                    newCal.add(Calendar.MONTH, -1)
                    calendar = newCal
                }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Previous Month",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = {
                    val newCal = calendar.clone() as Calendar
                    newCal.add(Calendar.MONTH, 1)
                    calendar = newCal
                }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Next Month",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Days of Week
        Row(modifier = Modifier.fillMaxWidth()) {
            val days = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
            days.forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Days Grid
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val firstDayOfWeek = (calendar.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1) }.get(Calendar.DAY_OF_WEEK) - 1
        
        val rows = (daysInMonth + firstDayOfWeek + 6) / 7
        
        for (row in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0 until 7) {
                    val dayIndex = row * 7 + col
                    val dayOfMonth = dayIndex - firstDayOfWeek + 1
                    
                    if (dayOfMonth in 1..daysInMonth) {
                        val currentDayCal = calendar.clone() as Calendar
                        currentDayCal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                        
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        val dateKey = sdf.format(currentDayCal.time)
                        val dayTrades = tradesByDate[dateKey] ?: emptyList()
                        
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(2.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (dayTrades.isNotEmpty()) {
                                        val dailyR = dayTrades.sumOf { it.rMultiple }
                                        if (dailyR == 0.0) ElectricCyan.copy(alpha = 0.12f)
                                        else if (dailyR > 0) EmeraldProfit.copy(alpha = 0.15f)
                                        else CrimsonLossBright.copy(alpha = 0.15f)
                                    } else Color.Transparent
                                )
                                .clickable { onDateSelected(currentDayCal.time, dayTrades) },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = dayOfMonth.toString(),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = if (dayTrades.isNotEmpty()) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = if (dayTrades.isNotEmpty()) {
                                        val dailyR = dayTrades.sumOf { it.rMultiple }
                                        if (dailyR == 0.0) ElectricCyan else if (dailyR > 0) EmeraldProfit else CrimsonLossBright
                                    } else MaterialTheme.colorScheme.onSurface
                                )
                                
                                if (dayTrades.isNotEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .size(4.dp)
                                            .clip(CircleShape)
                                            .background(ElectricCyan)
                                    )
                                }
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun DailySummaryCard(
    date: Date,
    trades: List<Trade>,
    onClose: () -> Unit
) {
    val sdf = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
    val totalPnl = trades.sumOf { it.netGainDollars }
    val winCount = trades.count { it.rMultiple > 0 }
    val lossCount = trades.count { it.rMultiple < 0 }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = sdf.format(date),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Close",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = ElectricCyan,
                modifier = Modifier.clickable { onClose() }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (trades.isEmpty()) {
            Text(
                text = "No trades executed on this day.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val totalRMultiple = trades.sumOf { it.rMultiple }
                val hasResults = trades.any { it.rMultiple != 0.0 }
                SummaryStat(label = "TRADES", value = trades.size.toString())
                if (hasResults) {
                    SummaryStat(
                        label = "NET R-GAIN",
                        value = "${if (totalRMultiple >= 0) "+" else ""}${String.format("%.1f", totalRMultiple)}R",
                        color = if (totalRMultiple >= 0) EmeraldProfit else CrimsonLossBright
                    )
                    SummaryStat(label = "W/L", value = "$winCount / $lossCount")
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "DAILY PERFORMANCE",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp, fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.outline
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            trades.forEach { trade ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        trade.rMultiple == 0.0 -> ElectricCyan
                                        trade.rMultiple > 0 -> EmeraldProfit
                                        else -> CrimsonLossBright
                                    }
                                )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${trade.pair} ${if (trade.direction == com.example.model.TradeDirection.LONG) "BUY" else "SELL"}",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (trade.rMultiple != 0.0) Text(
                        text = "${if (trade.rMultiple >= 0) "+" else ""}${trade.rMultiple}R",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                        color = if (trade.rMultiple >= 0) EmeraldProfit else CrimsonLossBright
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryStat(
    label: String,
    value: String,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            color = MaterialTheme.colorScheme.outline
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
            color = color
        )
    }
}
