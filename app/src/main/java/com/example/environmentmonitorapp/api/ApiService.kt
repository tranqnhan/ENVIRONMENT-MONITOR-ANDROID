package com.example.environmentmonitorapp.api

import retrofit2.Response
import retrofit2.http.POST

interface ApiService {
    @POST("server/auth")
    suspend fun authConnection(login: ServerLogin): Response<String>

    @POST("device/register")
    fun registerDevice(deviceRegistration: DeviceRegistration): Response<String>
}