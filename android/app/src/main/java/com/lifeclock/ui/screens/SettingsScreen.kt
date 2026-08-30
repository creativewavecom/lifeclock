package com.lifeclock.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifeclock.R
import com.lifeclock.domain.AppLanguage
import com.lifeclock.domain.SunriseSource
import com.lifeclock.domain.UpdateFrequency
import com.lifeclock.domain.WidgetTheme
import com.lifeclock.ui.MainViewModel
import com.lifeclock.ui.theme.AppPrimary
import com.lifeclock.ui.theme.AppTextPrimary
import com.lifeclock.ui.theme.AppTextSecondary
import java.util.Timer
import java.util.TimerTask

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val theme by viewModel.theme.collectAsStateWithLifecycle()
    val freq by viewModel.frequency.collectAsStateWithLifecycle()
    val src by viewModel.sunriseSource.collectAsStateWithLifecycle()
    val lang by viewModel.language.collectAsStateWithLifecycle()
    val notifEnabled by viewModel.notificationEnabled.collectAsStateWithLifecycle()
    val cities by viewModel.cities.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 32.dp,
                start = 16.dp, end = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Theme picker
            item {
                SectionHeader(stringResource(R.string.appearance))
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.widget_theme), color = AppTextSecondary, fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                ThemeGrid(theme) { viewModel.setTheme(it) }
            }

            // Notification settings
            item {
                SectionHeader(stringResource(R.string.notification_settings))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.enable_notification), color = AppTextPrimary, fontWeight = FontWeight.SemiBold)
                                Text(stringResource(R.string.enable_notification_summary), color = AppTextSecondary, fontSize = 12.sp)
                            }
                            Switch(checked = notifEnabled, onCheckedChange = { viewModel.setNotificationEnabled(it) })
                        }
                    }
                }
            }

            // City selection for notification
            if (notifEnabled && cities.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Notification city", color = AppTextSecondary, fontSize = 12.sp)
                            Spacer(Modifier.height(8.dp))
                            cities.forEach { city ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(
                                        selected = city.isHome,
                                        onClick = { viewModel.setNotificationCity(city.id) }
                                    )
                                    Text(city.name, color = AppTextPrimary, modifier = Modifier.weight(1f))
                                    Text(LifeClockCalculatorForSettings(city), color = AppPrimary, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Language
            item {
                SectionHeader(stringResource(R.string.language))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        AppLanguage.entries.forEach { language ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = lang == language,
                                    onClick = { viewModel.setLanguage(language) }
                                )
                                Text(
                                    text = when (language) {
                                        AppLanguage.ENGLISH -> stringResource(R.string.language_english)
                                        AppLanguage.PERSIAN -> stringResource(R.string.language_persian)
                                        AppLanguage.SYSTEM -> stringResource(R.string.language_system)
                                    },
                                    color = AppTextPrimary
                                )
                            }
                        }
                    }
                }
            }

            // Update frequency
            item {
                SectionHeader(stringResource(R.string.update_frequency))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        UpdateFrequency.entries.forEach { f ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = freq == f, onClick = { viewModel.setFrequency(f) })
                                Text(
                                    when (f) {
                                        UpdateFrequency.EVERY_30_SECONDS -> stringResource(R.string.update_30s)
                                        UpdateFrequency.EVERY_1_MINUTE -> stringResource(R.string.update_1m)
                                        UpdateFrequency.EVERY_5_MINUTES -> stringResource(R.string.update_5m)
                                    },
                                    color = AppTextPrimary
                                )
                            }
                        }
                    }
                }
            }

            // Sunrise source
            item {
                SectionHeader(stringResource(R.string.sunrise_source))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SunriseSource.entries.forEach { s ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = src == s, onClick = { viewModel.setSunriseSource(s) })
                                Text(
                                    when (s) {
                                        SunriseSource.OFFLINE -> stringResource(R.string.sunrise_offline)
                                        SunriseSource.API -> stringResource(R.string.sunrise_api)
                                        SunriseSource.HYBRID -> stringResource(R.string.sunrise_hybrid)
                                    },
                                    color = AppTextPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        color = AppTextPrimary,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
private fun ThemeGrid(selected: WidgetTheme, onSelect: (WidgetTheme) -> Unit) {
    // 2 columns of theme cards
    val themes = WidgetTheme.entries
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        themes.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { t ->
                    ThemeOption(
                        theme = t,
                        selected = t == selected,
                        onClick = { onSelect(t) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (row.size < 2) Box(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ThemeOption(
    theme: WidgetTheme,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = when (theme) {
        WidgetTheme.DIGITAL_MINIMAL -> androidx.compose.ui.graphics.Color(0xFF00E5FF)
        WidgetTheme.ANALOG -> AppPrimary
        WidgetTheme.NATURE_SUN -> androidx.compose.ui.graphics.Color(0xFFFA8F3F)
        WidgetTheme.PERSIAN_TRADITIONAL -> androidx.compose.ui.graphics.Color(0xFFF4D03F)
        WidgetTheme.GLASS_DARK -> androidx.compose.ui.graphics.Color.White
        WidgetTheme.MODERN_GRADIENT -> androidx.compose.ui.graphics.Color(0xFFFFD700)
    }
    val label = when (theme) {
        WidgetTheme.DIGITAL_MINIMAL -> stringResource(R.string.theme_digital_minimal)
        WidgetTheme.ANALOG -> stringResource(R.string.theme_analog)
        WidgetTheme.NATURE_SUN -> stringResource(R.string.theme_nature)
        WidgetTheme.PERSIAN_TRADITIONAL -> stringResource(R.string.theme_persian_traditional)
        WidgetTheme.GLASS_DARK -> stringResource(R.string.theme_glass_dark)
        WidgetTheme.MODERN_GRADIENT -> stringResource(R.string.theme_modern_gradient)
    }
    Card(
        modifier = modifier
            .height(80.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.surfaceVariant
            else MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = if (selected) 2.dp else 0.dp,
            color = if (selected) accent else Color.Transparent
        ),
        onClick = onClick
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(accent)
                )
                Spacer(Modifier.width(12.dp))
                Text(label, color = AppTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun LifeClockCalculatorForSettings(city: com.lifeclock.domain.City): String {
    val lastSunrise = com.lifeclock.domain.SunriseAnchor.lastSunrise(
        System.currentTimeMillis(), city.latitude, city.longitude
    )
    val lifeTime = com.lifeclock.domain.LifeClockCalculator.toLifeClock(
        System.currentTimeMillis(), city.timeZoneId, lastSunrise
    )
    return lifeTime.formatted
}

@Composable
private fun SettingsScreen_stringResource_unused() { /* placeholder */ }
