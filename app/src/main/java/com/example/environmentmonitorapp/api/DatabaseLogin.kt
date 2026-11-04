package com.example.environmentmonitorapp.api

import java.time.Instant

data class DatabaseLogin (
    val databasePassword: String,
    val timestamp: Instant
)