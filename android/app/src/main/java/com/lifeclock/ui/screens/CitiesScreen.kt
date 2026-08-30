package com.lifeclock.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifeclock.R
import com.lifeclock.domain.City
import com.lifeclock.domain.LifeClockCalculator
import com.lifeclock.ui.MainViewModel
import com.lifeclock.ui.theme.AppPrimary
import com.lifeclock.ui.theme.AppTextPrimary
import com.lifeclock.ui.theme.AppTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CitiesScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val cities by viewModel.cities.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.cities_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.useCurrentLocationAsHome() }) {
                        Icon(Icons.Default.LocationOn, contentDescription = stringResource(R.string.use_current_location))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = AppPrimary,
                contentColor = androidx.compose.ui.graphics.Color.Black
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 88.dp,
                start = 16.dp, end = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(cities) { city ->
                CityRow(city, viewModel)
            }
        }
    }

    if (showAddDialog) {
        AddCityDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { city ->
                viewModel.addCity(city)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun CityRow(city: City, viewModel: MainViewModel) {
    val now = remember { System.currentTimeMillis() }
    val lastSunrise = remember(city.id) {
        com.lifeclock.domain.SunriseAnchor.lastSunrise(now, city.latitude, city.longitude)
    }
    val lifeTime = LifeClockCalculator.toLifeClock(now, city.timeZoneId, lastSunrise)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(city.name, color = AppTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    text = "TZ: ${city.timeZoneId}  •  ${"%.2f".format(city.latitude)}, ${"%.2f".format(city.longitude)}",
                    color = AppTextSecondary,
                    fontSize = 11.sp
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = lifeTime.formatted,
                        color = AppPrimary,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Thin
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = lifeTime.period.key,
                        color = AppTextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
            if (city.isHome) {
                Text(
                    "HOME",
                    color = AppPrimary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun AddCityDialog(
    onDismiss: () -> Unit,
    onAdd: (City) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var tz by remember { mutableStateOf("Asia/Tehran") }
    var lat by remember { mutableStateOf("35.6892") }
    var lon by remember { mutableStateOf("51.3890") }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_city)) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.city_name)) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = tz,
                    onValueChange = { tz = it },
                    label = { Text(stringResource(R.string.timezone)) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Row {
                    OutlinedTextField(
                        value = lat,
                        onValueChange = { lat = it.filter { c -> c.isDigit() || c == '.' || c == '-' } },
                        label = { Text(stringResource(R.string.latitude)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(
                        value = lon,
                        onValueChange = { lon = it.filter { c -> c.isDigit() || c == '.' || c == '-' } },
                        label = { Text(stringResource(R.string.longitude)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            OutlinedButton(onClick = {
                val latD = lat.toDoubleOrNull() ?: 0.0
                val lonD = lon.toDoubleOrNull() ?: 0.0
                onAdd(City(0, name.ifBlank { "City" }, tz, latD, lonD))
            }) {
                Text(stringResource(R.string.add))
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun CitiesScreen_stringResource_unused() { /* placeholder */ }
