package com.lifeclock.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifeclock.R
import com.lifeclock.widget.WidgetConfig
import com.lifeclock.widget.WidgetPinner
import com.lifeclock.ui.theme.AppPrimary
import com.lifeclock.ui.theme.AppTextPrimary
import com.lifeclock.ui.theme.AppTextSecondary

/**
 * Size-picker dialog that lets the user choose a widget size and pin it
 * to the home screen without leaving the app.
 *
 * On Android 8+ this calls [WidgetPinner.requestPin] which launches the
 * system's "place widget" UI. The user then confirms where to drop the
 * widget, just like dragging from the launcher's widget panel.
 *
 * On older devices, we fall back to opening the system widget picker.
 */
@Composable
fun AddWidgetDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Widget", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    text = "Choose a size to add to your home screen:",
                    color = AppTextSecondary,
                    fontSize = 13.sp
                )
                Spacer(Modifier.height(12.dp))

                WidgetSizeOption(
                    title = "Prayer  (4×3)",
                    subtitle = "Prayer times + life clock + dates",
                    highlighted = true,
                    onClick = {
                        WidgetPinner.requestPin(context, WidgetConfig.SIZE_PRAYER)
                        onDismiss()
                    }
                )
                Spacer(Modifier.height(8.dp))
                WidgetSizeOption(
                    title = "Small  (2×2)",
                    subtitle = "Just the life clock + city + period",
                    onClick = {
                        WidgetPinner.requestPin(context, WidgetConfig.SIZE_SMALL)
                        onDismiss()
                    }
                )
                Spacer(Modifier.height(8.dp))
                WidgetSizeOption(
                    title = "Bar  (4×1)",
                    subtitle = "Horizontal: city • life • official",
                    onClick = {
                        WidgetPinner.requestPin(context, WidgetConfig.SIZE_WIDE)
                        onDismiss()
                    }
                )
                Spacer(Modifier.height(8.dp))
                WidgetSizeOption(
                    title = "Medium  (4×2)",
                    subtitle = "Life clock + progress + sunrise/sunset",
                    onClick = {
                        WidgetPinner.requestPin(context, WidgetConfig.SIZE_MEDIUM)
                        onDismiss()
                    }
                )
                Spacer(Modifier.height(8.dp))
                WidgetSizeOption(
                    title = "Large  (4×4)",
                    subtitle = "Everything incl. Persian date + day length",
                    onClick = {
                        WidgetPinner.requestPin(context, WidgetConfig.SIZE_LARGE)
                        onDismiss()
                    }
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(stringResourceRes(R.string.cancel))
            }
        }
    )
}

@Composable
private fun WidgetSizeOption(
    title: String,
    subtitle: String,
    highlighted: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(
                width = if (highlighted) 2.dp else 1.dp,
                color = if (highlighted) AppPrimary else AppPrimary.copy(alpha = 0.2f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Mini preview
            Box(
                modifier = Modifier
                    .size(36.dp, 36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(AppPrimary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp, 4.dp)
                        .background(AppPrimary)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, color = AppTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    if (highlighted) {
                        Spacer(Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(AppPrimary)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("NEW", color = androidx.compose.ui.graphics.Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Text(subtitle, color = AppTextSecondary, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun stringResourceRes(id: Int): String = androidx.compose.ui.res.stringResource(id)
