package com.example.environmentmonitorapp.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiService {
    @POST("server/auth")
    suspend fun authConnection(@Body auth: AuthRequest): Response<Unit>

    @POST("device/register")
    suspend fun registerMonitorDevice(@Body monitorDeviceRegistration: MonitorDeviceRegistration): Response<Unit>

    @GET("device/usernames")
    suspend fun getAllMonitorDeviceUsernames(): Response<List<String>>

    @GET("data/current")
    suspend fun getCurrentEnvironmentData(@Query("monitorDeviceUsername") monitorDeviceUsername: String): Response<EnvironmentDataPoint>

    @POST("data/range")
    suspend fun getEnvironmentDataHistory(@Body environmentDataRange: EnvironmentDataRange): Response<List<EnvironmentDataPoint>>
}