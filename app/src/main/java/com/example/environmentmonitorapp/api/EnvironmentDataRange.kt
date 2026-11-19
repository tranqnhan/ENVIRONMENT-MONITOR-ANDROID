package com.example.environmentmonitorapp.api

import java.time.Instant

data class EnvironmentDataRange (
    val monitorDeviceUsername: String,
    val startTime: Instant,
    val endTime: Instant
)