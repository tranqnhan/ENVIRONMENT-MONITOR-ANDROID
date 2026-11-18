package com.example.environmentmonitorapp.api

import android.content.Context
import com.example.environmentmonitorapp.core.SettingsDatastore
import com.example.environmentmonitorapp.core.TransformURL
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

object RetrofitClient {
    private var serverURL: String? = null
    private var api: ApiService? = null

    suspend fun getAPI(context: Context): ApiService {
        registerAPI(SettingsDatastore(context).getHTTPSServerURL())
        return api!!
    }

     fun getAPI(baseUrl: String): ApiService {
        registerAPI(baseUrl)
        return api!!
    }

    private fun registerAPI(baseUrl: String) {
        require(baseUrl.isNotBlank()) {
            "Server URL must not be blank"
        }

        if (serverURL != baseUrl) {
            api = Retrofit.Builder()
                .baseUrl(TransformURL.normalizeBaseURL(baseUrl))
                .addConverterFactory(MoshiConverterFactory.create(MoshiAdapterObject.moshi))
                .build()
                .create(ApiService::class.java)
            serverURL = baseUrl
        }
    }
}