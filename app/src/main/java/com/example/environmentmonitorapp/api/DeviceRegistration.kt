package com.example.environmentmonitorapp.api

import java.time.Instant

data class DeviceRegistration (
    val databasePassword: String,
    val deviceUsername: String,
    val devicePassword: String,
    val timestamp: Instant
)