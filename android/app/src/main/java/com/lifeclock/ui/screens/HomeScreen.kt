package com.lifeclock.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifeclock.R
import com.lifeclock.domain.City
import com.lifeclock.domain.AppLanguage
import com.lifeclock.domain.LifeClockCalculator
import com.lifeclock.domain.LifeClockPeriod
import com.lifeclock.domain.PersianCalendar
import com.lifeclock.domain.SunriseCalculator
import com.lifeclock.domain.SunriseAnchor
import com.lifeclock.domain.TimeFormatter
import com.lifeclock.domain.WidgetTheme
import com.lifeclock.ui.MainViewModel
import com.lifeclock.ui.components.LifeClockPreviewCard
import com.lifeclock.ui.theme.AccentAnalog
import com.lifeclock.ui.theme.AccentDigital
import com.lifeclock.ui.theme.AccentGlass
import com.lifeclock.ui.theme.AccentGradient
import com.lifeclock.ui.theme.AccentNature
import com.lifeclock.ui.theme.AccentPersian
import com.lifeclock.ui.theme.AppPrimary
import com.lifeclock.ui.theme.AppSurfaceVariant
import com.lifeclock.ui.theme.AppTextPrimary
import com.lifeclock.ui.theme.AppTextSecondary
import com.lifeclock.ui.theme.GradientEnd
import com.lifeclock.ui.theme.GradientStart
import com.lifeclock.ui.theme.NatureDawn
import com.lifeclock.ui.theme.NatureDusk
import com.lifeclock.ui.theme.NatureMorning
import com.lifeclock.ui.theme.NatureNight
import com.lifeclock.ui.theme.NatureNoon
import com.lifeclock.ui.theme.PersianBg
import com.lifeclock.ui.theme.PersianText
import java.util.Timer
import java.util.TimerTask

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onNavigateToSettings: () -> Unit,
    onNavigateToCities: () -> Unit,
    onAddWidget: () -> Unit
) {
    val cities by viewModel.cities.collectAsStateWithLifecycle()
    val theme by viewModel.theme.collectAsStateWithLifecycle()

    // Tick every second so the preview life clock is live
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        val timer = Timer()
        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() { now = System.currentTimeMillis() }
        }, 0, 1000)
    }

    // Seed preset cities on first run
    LaunchedEffect(Unit) { viewModel.seedPresetsIfEmpty() }

    val homeCity = cities.firstOrNull { it.isHome }
    val otherCities = cities.filterNot { it.isHome }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.home_title),
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshAllNow() }) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.refresh_now))
                    }
                    IconButton(onClick = onAddWidget) {
                        Icon(Icons.Default.Add, contentDescription = "Add widget")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToCities,
                containerColor = AppPrimary,
                contentColor = Color.Black
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_city))
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (cities.isEmpty()) {
            EmptyState(padding)
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = padding.calculateTopPadding() + 8.dp,
                    bottom = padding.calculateBottomPadding() + 88.dp,
                    start = 16.dp, end = 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // === HOME CITY (large card at top) ===
                homeCity?.let { home ->
                    item {
                        Text(
                            text = "★ " + stringResource(R.string.home_title),
                            color = AppPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                        HomeCityCard(
                            city = home,
                            theme = theme,
                            now = now,
                            onAddWidget = onAddWidget
                        )
                    }
                }

                // === OTHER CITIES ===
                if (otherCities.isNotEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.cities_title),
                            color = AppTextSecondary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                        )
                    }
                    items(otherCities) { city ->
                        OtherCityCard(
                            city = city,
                            now = now,
                            onSetHome = { viewModel.setHomeCity(city) },
                            onDelete = { viewModel.removeCity(city) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeCityCard(
    city: City,
    theme: WidgetTheme,
    now: Long,
    onAddWidget: () -> Unit
) {
    val lastSunrise = remember(city.id) {
        SunriseAnchor.lastSunrise(System.currentTimeMillis(), city.latitude, city.longitude)
    }
    val lifeTime = LifeClockCalculator.toLifeClock(now, city.timeZoneId, lastSunrise)
    val official = LifeClockCalculator.formatOfficialTime(now, city.timeZoneId)

    val accent = when (theme) {
        WidgetTheme.DIGITAL_MINIMAL -> AccentDigital
        WidgetTheme.ANALOG -> AccentAnalog
        WidgetTheme.NATURE_SUN -> AccentNature
        WidgetTheme.PERSIAN_TRADITIONAL -> AccentPersian
        WidgetTheme.GLASS_DARK -> AccentGlass
        WidgetTheme.MODERN_GRADIENT -> AccentGradient
    }

    val bgBrush: Brush = when (theme) {
        WidgetTheme.DIGITAL_MINIMAL, WidgetTheme.ANALOG ->
            androidx.compose.ui.graphics.SolidColor(Color(0xFF1A1A1E))
        WidgetTheme.NATURE_SUN -> androidx.compose.ui.graphics.SolidColor(natureBackgroundForPeriod(lifeTime.period))
        WidgetTheme.PERSIAN_TRADITIONAL -> androidx.compose.ui.graphics.SolidColor(PersianBg)
        WidgetTheme.GLASS_DARK -> androidx.compose.ui.graphics.SolidColor(Color(0x99000000))
        WidgetTheme.MODERN_GRADIENT ->
            Brush.linearGradient(listOf(GradientStart, GradientEnd))
    }

    val textColor = when (theme) {
        WidgetTheme.PERSIAN_TRADITIONAL -> PersianText
        else -> Color.White
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .background(bgBrush, RoundedCornerShape(24.dp))
                .border(
                    width = 1.dp,
                    color = accent.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(24.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // City name row + add widget button
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = city.name.uppercase(),
                        color = textColor.copy(alpha = 0.7f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = official,
                        color = accent,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = onAddWidget,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Add widget",
                            tint = textColor.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Life clock big
                Text(
                    text = lifeTime.formatted,
                    color = textColor,
                    fontSize = 80.sp,
                    fontWeight = FontWeight.Thin,
                    letterSpacing = 1.sp
                )

                // Period + label
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.life_clock),
                        color = textColor.copy(alpha = 0.5f),
                        fontSize = 11.sp,
                        letterSpacing = 2.sp
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "• ${lifeTime.period.key.uppercase()}",
                        color = accent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.height(20.dp))

                // Day progress
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        stringResource(R.string.day_progress),
                        color = textColor.copy(alpha = 0.5f),
                        fontSize = 11.sp
                    )
                    Text(
                        text = TimeFormatter.formatDayProgress(lifeTime.dayProgressRatio, AppLanguage.ENGLISH),
                        color = textColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(textColor.copy(alpha = 0.15f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(lifeTime.dayProgressRatio.toFloat().coerceIn(0f, 1f))
                            .height(5.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(accent)
                    )
                }

                Spacer(Modifier.height(16.dp))

                // Sunrise / Sunset row
                Row(modifier = Modifier.fillMaxWidth()) {
                    val solarTimes = SunriseCalculator.compute(now, city.latitude, city.longitude)
                    InfoColumn(
                        icon = "▲",
                        iconColor = accent,
                        label = stringResource(R.string.sunrise),
                        value = lastSunrise?.let {
                            TimeFormatter.formatHourMinute(it, city.timeZoneId, AppLanguage.ENGLISH)
                        } ?: "—",
                        textColor = textColor
                    )
                    InfoColumn(
                        icon = "☀",
                        iconColor = accent,
                        label = stringResource(R.string.day_length),
                        value = TimeFormatter.formatDuration(solarTimes.dayLengthMinutes, AppLanguage.ENGLISH),
                        textColor = textColor
                    )
                    InfoColumn(
                        icon = "▼",
                        iconColor = accent,
                        label = stringResource(R.string.sunset),
                        value = solarTimes.sunsetUtcMillis?.let {
                            TimeFormatter.formatHourMinute(it, city.timeZoneId, AppLanguage.ENGLISH)
                        } ?: "—",
                        textColor = textColor
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.InfoColumn(
    icon: String,
    iconColor: Color,
    label: String,
    value: String,
    textColor: Color
) {
    Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(icon, color = iconColor, fontSize = 14.sp)
        Text(
            value,
            color = textColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            label,
            color = textColor.copy(alpha = 0.5f),
            fontSize = 9.sp,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
private fun OtherCityCard(
    city: City,
    now: Long,
    onSetHome: () -> Unit,
    onDelete: () -> Unit
) {
    val lastSunrise = remember(city.id) {
        SunriseAnchor.lastSunrise(System.currentTimeMillis(), city.latitude, city.longitude)
    }
    val lifeTime = LifeClockCalculator.toLifeClock(now, city.timeZoneId, lastSunrise)
    val official = LifeClockCalculator.formatOfficialTime(now, city.timeZoneId)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    city.name,
                    color = AppTextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Official $official  •  ${lifeTime.period.key}",
                    color = AppTextSecondary,
                    fontSize = 11.sp
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = lifeTime.formatted,
                    color = AppPrimary,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Thin
                )
            }
            IconButton(onClick = onSetHome) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = "Set home",
                    tint = AppTextSecondary
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AppTextSecondary)
            }
        }
    }
}

@Composable
private fun EmptyState(padding: PaddingValues) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.LocationOn, contentDescription = null, tint = AppPrimary, modifier = Modifier.size(72.dp))
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.no_cities_yet),
            color = AppTextSecondary,
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )
    }
}

private fun natureBackgroundForPeriod(period: LifeClockPeriod): Color = when (period) {
    LifeClockPeriod.SUNRISE -> NatureDawn
    LifeClockPeriod.MORNING -> NatureMorning
    LifeClockPeriod.NOON -> NatureNoon
    LifeClockPeriod.AFTERNOON -> NatureNoon
    LifeClockPeriod.SUNSET -> NatureDusk
    LifeClockPeriod.EVENING -> NatureDusk
    LifeClockPeriod.NIGHT -> NatureNight
}
