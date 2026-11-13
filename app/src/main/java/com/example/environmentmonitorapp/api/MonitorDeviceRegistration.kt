package com.example.environmentmonitorapp.api

data class MonitorDeviceRegistration (
    val serverAdminPassword: String,
    val monitorDeviceUsername: String,
    val monitorDevicePassword: String,
)