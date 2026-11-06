package com.example.environmentmonitorapp.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("server/auth")
    suspend fun authConnection(@Body auth: AuthRequest): Response<Void>

    @POST("device/register")
    fun registerDevice(@Body deviceRegistration: DeviceRegistration): Response<String>
}