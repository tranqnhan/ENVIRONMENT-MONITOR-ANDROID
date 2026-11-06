package com.example.environmentmonitorapp.api

import android.content.Context
import com.example.environmentmonitorapp.core.SettingsDatastore
import com.example.environmentmonitorapp.core.dataStore
import kotlinx.coroutines.flow.first
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private var serverURL: String? = null
    private var api: ApiService? = null

    suspend fun getAPI(context: Context): ApiService {
        val baseUrl = SettingsDatastore(context).getServerURL()

        require(!baseUrl.isNullOrBlank()) {
            "Server URL must be set in datastore"
        }


        if (serverURL != baseUrl) {
            api = Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ApiService::class.java)
            serverURL = baseUrl
        }

        return api!!
    }

     fun getAPI(baseUrl: String): ApiService {
        require(baseUrl.isNotBlank()) {
            "Server URL must not be blank"
        }

        if (serverURL != baseUrl) {
            api = Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ApiService::class.java)
            serverURL = baseUrl
        }

        return api!!
    }
}