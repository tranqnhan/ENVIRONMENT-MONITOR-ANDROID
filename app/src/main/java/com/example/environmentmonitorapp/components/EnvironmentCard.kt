package com.example.environmentmonitorapp.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.environmentmonitorapp.api.EnvironmentDataPoint
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale


@Composable
fun EnvironmentCard(
    monitorDeviceName: String,
    data: EnvironmentDataPoint,
    isConnect: Boolean,
    modifier: Modifier = Modifier
) {
    val measuredTime = DateTimeFormatter
        .ofPattern("MMM d, HH:mm:ss")
        .withZone(ZoneId.systemDefault())
        .format(data.measuredAt)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatusCircleLight(if (isConnect) Color(0xFF22C55E) else Color(0xFFEF4444))

                Text(
                    text = monitorDeviceName,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            }

            HorizontalDivider()

            EnvironmentReading(
                label = "Temperature",
                value = String.format(
                    Locale.getDefault(),
                    "%.1f °C",
                    data.temperature
                ),
                color = EnvironmentDataColor[EnvironmentDataType.TEMPERATURE]!!
            )

            EnvironmentReading(
                label = "Humidity",
                value = String.format(
                    Locale.getDefault(),
                    "%.1f %%",
                    data.humidity
                ),
                color = EnvironmentDataColor[EnvironmentDataType.HUMIDITY]!!
            )

            EnvironmentReading(
                label = "CO₂",
                value = "${data.co2Level} ppm",
                color = EnvironmentDataColor[EnvironmentDataType.CO2]!!
            )

            HorizontalDivider()

            Text(
                text = "Measured: $measuredTime",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EnvironmentReading(
    label: String,
    value: String,
    color: Color = Color(0xFF00897B)
) {
    val opacity = remember { Animatable(1f) }

    LaunchedEffect(value) {
        opacity.snapTo(0.25f)
        opacity.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1000)
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color = color
        )

        Spacer(Modifier.width(16.dp))

        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = color,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.graphicsLayer {
                alpha = opacity.value
            }
        )
    }
}