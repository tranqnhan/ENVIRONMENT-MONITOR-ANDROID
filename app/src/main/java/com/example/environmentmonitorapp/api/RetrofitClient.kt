package com.example.environmentmonitorapp.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    // TODO: change the base url to
    private var serverBaseURL = ""

    private var api: ApiService? = null

    fun init(serverURL: String) {
        serverBaseURL = serverURL
        api = Retrofit.Builder()
            .baseUrl(serverBaseURL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    fun getAPI(): ApiService {
        return api!!
    }

}
