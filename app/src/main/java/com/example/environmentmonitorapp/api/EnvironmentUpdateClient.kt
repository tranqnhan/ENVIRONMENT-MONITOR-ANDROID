package com.example.environmentmonitorapp.api

import android.content.Context
import com.example.environmentmonitorapp.core.SettingsDatastore
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import org.hildan.krossbow.stomp.StompClient
import org.hildan.krossbow.stomp.subscribeText
import org.hildan.krossbow.websocket.okhttp.OkHttpWebSocketClient

class EnvironmentUpdateClient {
    private val client = StompClient(OkHttpWebSocketClient())
    private val adapter = MoshiAdapterObject.moshi.adapter(EnvironmentDataPoint::class.java)

    suspend fun listen(context: Context, topicEndpoint: String, monitorDeviceUsername: String, onUpdate: suspend (EnvironmentDataPoint) -> Unit) {
        val serverURL: String = SettingsDatastore(context).getWSSServerURL()
        val session = client.connect("${serverURL}ws")

        try {
            session
                .subscribeText("/topic/$monitorDeviceUsername/$topicEndpoint")
                .collect { message ->
                    val update = requireNotNull(adapter.fromJson(message))
                    onUpdate(update)
                }
        } finally {
            withContext(NonCancellable) {
                session.disconnect()
            }
        }
    }
}