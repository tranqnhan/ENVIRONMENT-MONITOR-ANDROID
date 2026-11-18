package com.example.environmentmonitorapp.api

import java.time.Instant

data class CurrentEnvironmentUpdateGet (
    val temperature: Double,
    val humidity: Double,
    val co2Level: Int,
    val measuredAt: Instant
)