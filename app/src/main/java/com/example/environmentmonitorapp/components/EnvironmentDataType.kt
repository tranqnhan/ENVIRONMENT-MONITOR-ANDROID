package com.example.environmentmonitorapp.components

import androidx.compose.ui.graphics.Color

enum class EnvironmentDataType {
    CO2,
    TEMPERATURE,
    HUMIDITY
}

val EnvironmentDataColor: Map<EnvironmentDataType, Color> = mapOf(
    EnvironmentDataType.CO2 to Color(0xFFCE93D8),
    EnvironmentDataType.TEMPERATURE to Color(0xFFFFAB91),
    EnvironmentDataType.HUMIDITY to Color(0xFF90CAF9)
)