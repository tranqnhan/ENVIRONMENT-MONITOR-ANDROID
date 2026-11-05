package com.example.environmentmonitorapp.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    val api: ApiService by lazy {
        val serverBaseURL = ""

        Retrofit.Builder()
            .baseUrl(serverBaseURL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}