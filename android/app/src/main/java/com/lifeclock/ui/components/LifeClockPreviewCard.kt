package com.lifeclock.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifeclock.domain.City
import com.lifeclock.domain.LifeClockCalculator
import com.lifeclock.domain.LifeClockPeriod
import com.lifeclock.domain.LifeClockTime
import com.lifeclock.domain.PersianCalendar
import com.lifeclock.domain.TimeFormatter
import com.lifeclock.domain.WidgetTheme
import com.lifeclock.ui.theme.AccentAnalog
import com.lifeclock.ui.theme.AccentDigital
import com.lifeclock.ui.theme.AccentGlass
import com.lifeclock.ui.theme.AccentGradient
import com.lifeclock.ui.theme.AccentNature
import com.lifeclock.ui.theme.AccentPersian
import com.lifeclock.ui.theme.AppPrimary
import com.lifeclock.ui.theme.AppSurfaceVariant
import com.lifeclock.ui.theme.GradientEnd
import com.lifeclock.ui.theme.GradientStart
import com.lifeclock.ui.theme.NatureDawn
import com.lifeclock.ui.theme.NatureDusk
import com.lifeclock.ui.theme.NatureMorning
import com.lifeclock.ui.theme.NatureNight
import com.lifeclock.ui.theme.NatureNoon
import com.lifeclock.ui.theme.PersianBg
import com.lifeclock.ui.theme.PersianText
import org.joda.time.DateTime

/**
 * Previews the life clock with a chosen widget theme — used in the in-app
 * settings to let the user pick the theme before placing the widget.
 *
 * The preview mirrors the look of the actual widget as closely as possible
 * (same accent colors, same gradient direction, same typography hierarchy).
 */
@Composable
fun LifeClockPreviewCard(
    city: City?,
    theme: WidgetTheme,
    realUtcMillis: Long,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val lifeClock = if (city != null) {
        // Use the most recent sunrise as the life-clock anchor — this is what
        // the widget actually displays.
        val lastSunrise = com.lifeclock.domain.SunriseAnchor.lastSunrise(
            realUtcMillis, city.latitude, city.longitude
        )
        LifeClockCalculator.toLifeClock(realUtcMillis, city.timeZoneId, lastSunrise)
    } else null

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
            SolidColor(Color(0xFF1A1A1E))
        WidgetTheme.NATURE_SUN -> {
            val bg = lifeClock?.let { natureBackgroundForPeriod(it.period) } ?: NatureDawn
            SolidColor(bg)
        }
        WidgetTheme.PERSIAN_TRADITIONAL ->
            SolidColor(PersianBg)
        WidgetTheme.GLASS_DARK ->
            SolidColor(Color(0x99000000))
        WidgetTheme.MODERN_GRADIENT ->
            Brush.linearGradient(listOf(GradientStart, GradientEnd))
    }

    val textColor = when (theme) {
        WidgetTheme.PERSIAN_TRADITIONAL -> PersianText
        else -> Color.White
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick?.invoke() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .background(bgBrush, RoundedCornerShape(20.dp))
                .padding(20.dp)
                .border(
                    width = if (theme == WidgetTheme.PERSIAN_TRADITIONAL) 2.dp else 1.dp,
                    color = accent.copy(alpha = if (theme == WidgetTheme.GLASS_DARK) 0.5f else 0.4f),
                    shape = RoundedCornerShape(20.dp)
                )
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // City name
                Text(
                    text = city?.name?.uppercase() ?: "—",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Spacer(Modifier.height(8.dp))

                // Life clock big
                Text(
                    text = lifeClock?.formatted ?: "--:--",
                    color = textColor,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Spacer(Modifier.height(2.dp))

                // Period badge
                Text(
                    text = "• ${lifeClock?.period?.key?.uppercase() ?: ""}",
                    color = accent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(10.dp))

                // Official time
                city?.let {
                    val official = LifeClockCalculator.formatOfficialTime(realUtcMillis, it.timeZoneId)
                    Text(
                        text = "$official  •  Official",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }

                // Day progress bar
                if (lifeClock != null) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Day Progress",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 10.sp
                        )
                        Text(
                            text = "${(lifeClock.dayProgressRatio * 100).toInt()}%",
                            color = textColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.White.copy(alpha = 0.2f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(lifeClock.dayProgressRatio.toFloat())
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(accent)
                        )
                    }
                }
            }
        }
    }
}

/** Choose a nature background color based on the life clock period. */
private fun natureBackgroundForPeriod(period: LifeClockPeriod): Color = when (period) {
    LifeClockPeriod.SUNRISE -> NatureDawn
    LifeClockPeriod.MORNING -> NatureMorning
    LifeClockPeriod.NOON -> NatureNoon
    LifeClockPeriod.AFTERNOON -> NatureNoon
    LifeClockPeriod.SUNSET -> NatureDusk
    LifeClockPeriod.EVENING -> NatureDusk
    LifeClockPeriod.NIGHT -> NatureNight
}
