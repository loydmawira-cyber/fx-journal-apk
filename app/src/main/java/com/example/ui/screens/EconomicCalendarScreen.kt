package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CrimsonLossBright
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldProfit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private data class CalendarEvent(
    val title: String,
    val currency: String,
    val impact: String,
    val timeMillis: Long,
    val forecast: String,
    val previous: String
)

private object CalendarCache {
    var events: List<CalendarEvent> = emptyList()
    var fetchedAt: Long = 0L
}

private const val CALENDAR_URL = "https://nfs.faireconomy.media/ff_calendar_thisweek.json"
private const val CACHE_MS = 10 * 60 * 1000L

private suspend fun loadCalendar(force: Boolean): Result<List<CalendarEvent>> = withContext(Dispatchers.IO) {
    val now = System.currentTimeMillis()
    if (!force && CalendarCache.events.isNotEmpty() && now - CalendarCache.fetchedAt < CACHE_MS) {
        return@withContext Result.success(CalendarCache.events)
    }
    try {
        val conn = URL(CALENDAR_URL).openConnection() as HttpURLConnection
        conn.connectTimeout = 10_000
        conn.readTimeout = 10_000
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Android) FxJournal")
        if (conn.responseCode != 200) error("Server returned ${conn.responseCode}")
        val body = conn.inputStream.bufferedReader().use { it.readText() }
        val arr = JSONArray(body)
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)
        val list = (0 until arr.length()).mapNotNull { i ->
            val o = arr.getJSONObject(i)
            val time = runCatching { parser.parse(o.optString("date"))?.time }.getOrNull() ?: return@mapNotNull null
            CalendarEvent(
                title = o.optString("title"),
                currency = o.optString("country"),
                impact = o.optString("impact"),
                timeMillis = time,
                forecast = o.optString("forecast"),
                previous = o.optString("previous")
            )
        }.sortedBy { it.timeMillis }
        CalendarCache.events = list
        CalendarCache.fetchedAt = now
        Result.success(list)
    } catch (e: Exception) {
        // Fall back to the last good copy if we have one
        if (CalendarCache.events.isNotEmpty()) Result.success(CalendarCache.events) else Result.failure(e)
    }
}

private const val UI_PREFS = "fx_journal_ui"
private const val TZ_KEY = "calendar_tz"

private fun gmtLabel(zone: TimeZone, atMillis: Long): String {
    val mins = zone.getOffset(atMillis) / 60_000
    val sign = if (mins >= 0) "+" else "-"
    val abs = kotlin.math.abs(mins)
    return "GMT%s%02d:%02d".format(sign, abs / 60, abs % 60)
}

private data class TzOption(val id: String, val offsetMinutes: Int, val label: String)

private fun buildTimeZoneOptions(): List<TzOption> {
    val now = System.currentTimeMillis()
    return TimeZone.getAvailableIDs()
        .filter { (it.contains("/") && !it.startsWith("Etc/") && !it.startsWith("SystemV/")) || it == "UTC" }
        .map { id ->
            val zone = TimeZone.getTimeZone(id)
            TzOption(id, zone.getOffset(now) / 60_000, "${gmtLabel(zone, now)}  •  ${id.replace('_', ' ')}")
        }
        .sortedWith(compareBy({ it.offsetMinutes }, { it.id }))
}

private fun impactColor(impact: String): Color = when (impact) {
    "High" -> CrimsonLossBright
    "Medium" -> Color(0xFFFFB020)
    "Low" -> Color(0xFFE6D34A)
    else -> Color.Gray
}

@Composable
fun EconomicCalendarScreen() {
    var events by remember { mutableStateOf<List<CalendarEvent>>(CalendarCache.events) }
    var loading by remember { mutableStateOf(CalendarCache.events.isEmpty()) }
    var error by remember { mutableStateOf<String?>(null) }
    var refreshTick by remember { mutableIntStateOf(0) }
    var impactFilter by remember { mutableStateOf("All") }

    LaunchedEffect(refreshTick) {
        loading = true
        error = null
        loadCalendar(force = refreshTick > 0).fold(
            onSuccess = { events = it },
            onFailure = { error = it.message ?: "Could not load the calendar" }
        )
        loading = false
    }

    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(UI_PREFS, android.content.Context.MODE_PRIVATE) }
    // null = follow the phone's timezone
    var tzId by remember { mutableStateOf(prefs.getString(TZ_KEY, null)) }
    var showTzPicker by remember { mutableStateOf(false) }
    val activeZone = remember(tzId) { tzId?.let { TimeZone.getTimeZone(it) } ?: TimeZone.getDefault() }
    val dayFmt = remember(activeZone) { SimpleDateFormat("EEEE, d MMM", Locale.getDefault()).apply { timeZone = activeZone } }
    val timeFmt = remember(activeZone) { SimpleDateFormat("HH:mm", Locale.getDefault()).apply { timeZone = activeZone } }
    val zoneName = (if (tzId == null) "Phone time" else activeZone.id.substringAfterLast('/').replace('_', ' ')) +
        " (" + gmtLabel(activeZone, System.currentTimeMillis()) + ")"

    val visible = events.filter { impactFilter == "All" || it.impact == impactFilter }
    val grouped = visible.groupBy { dayFmt.format(it.timeMillis) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("economic_calendar_screen")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp, top = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Economic Calendar",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "This week • all times shown in the timezone below",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = { refreshTick++ }, modifier = Modifier.testTag("calendar_refresh")) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = ElectricCyan)
            }
        }

        Row(
            modifier = Modifier
                .padding(start = 16.dp, top = 10.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .clickable { showTzPicker = true }
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .testTag("calendar_timezone_button"),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Public, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text = zoneName,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("All", "High", "Medium", "Low").forEach { level ->
                val selected = impactFilter == level
                Text(
                    text = level,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (selected) Color.Black else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selected) ElectricCyan else MaterialTheme.colorScheme.surfaceContainerHigh)
                        .clickable { impactFilter = level }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                )
            }
        }

        when {
            loading && events.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = ElectricCyan)
            }
            error != null && events.isEmpty() -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Couldn't load the calendar", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(error ?: "", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Try again",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(ElectricCyan)
                            .clickable { refreshTick++ }
                            .padding(horizontal = 18.dp, vertical = 8.dp)
                    )
                }
            }
            grouped.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No events for this filter", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                grouped.forEach { (day, dayEvents) ->
                    item(key = "day_$day") {
                        Text(
                            text = day,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = ElectricCyan,
                            modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)
                        )
                    }
                    items(dayEvents, key = { "${it.timeMillis}_${it.currency}_${it.title}" }) { ev ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.width(58.dp)) {
                                Text(
                                    text = timeFmt.format(ev.timeMillis),
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .size(9.dp)
                                        .clip(CircleShape)
                                        .background(impactColor(ev.impact))
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = ev.currency,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = ElectricCyan,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(ElectricCyan.copy(alpha = 0.15f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = ev.title,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (ev.forecast.isNotBlank() || ev.previous.isNotBlank()) {
                                    Spacer(Modifier.height(4.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                        if (ev.forecast.isNotBlank()) Text(
                                            text = "Forecast ${ev.forecast}",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                            color = EmeraldProfit
                                        )
                                        if (ev.previous.isNotBlank()) Text(
                                            text = "Previous ${ev.previous}",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
    if (showTzPicker) {
        TimeZonePickerSheet(
            selectedId = tzId,
            onSelect = { id ->
                tzId = id
                prefs.edit().apply { if (id == null) remove(TZ_KEY) else putString(TZ_KEY, id) }.apply()
                showTzPicker = false
            },
            onDismiss = { showTzPicker = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeZonePickerSheet(selectedId: String?, onSelect: (String?) -> Unit, onDismiss: () -> Unit) {
    val options = remember { buildTimeZoneOptions() }
    var query by remember { mutableStateOf("") }
    val filtered = remember(query) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) options else options.filter { it.label.lowercase().contains(q) }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(modifier = Modifier.fillMaxHeight(0.92f).padding(horizontal = 16.dp)) {
            Text(
                text = "Choose timezone",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                placeholder = { Text("Search city, country or GMT+3") },
                modifier = Modifier.fillMaxWidth().testTag("timezone_search")
            )
            Spacer(Modifier.height(8.dp))
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                if (query.isBlank()) {
                    item(key = "device") {
                        TzRow(
                            text = "Use my phone's timezone (automatic)",
                            selected = selectedId == null,
                            onClick = { onSelect(null) }
                        )
                    }
                }
                items(filtered, key = { it.id }) { option ->
                    TzRow(text = option.label, selected = option.id == selectedId, onClick = { onSelect(option.id) })
                }
            }
        }
    }
}

@Composable
private fun TzRow(text: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal),
            color = if (selected) ElectricCyan else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        if (selected) Icon(Icons.Default.Check, contentDescription = "Selected", tint = ElectricCyan, modifier = Modifier.size(18.dp))
    }
}
