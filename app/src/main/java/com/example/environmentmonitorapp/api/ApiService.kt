package com.example.environmentmonitorapp.api

import retrofit2.Call
import retrofit2.http.POST

interface ApiService {
    @POST("verify")
    fun verifyConnection(): Call<DatabaseLogin>

    @POST("device/register")
    fun registerDevice(): Call<DeviceRegistration>
}