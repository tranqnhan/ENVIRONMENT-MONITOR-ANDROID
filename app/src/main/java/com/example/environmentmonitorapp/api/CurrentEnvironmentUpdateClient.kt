package com.example.environmentmonitorapp.api

import android.content.Context
import com.example.environmentmonitorapp.core.SettingsDatastore
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import org.hildan.krossbow.stomp.StompClient
import org.hildan.krossbow.stomp.subscribeText
import org.hildan.krossbow.websocket.okhttp.OkHttpWebSocketClient

class CurrentEnvironmentUpdateClient {
    private val client = StompClient(OkHttpWebSocketClient())
    private val adapter = MoshiAdapterObject.moshi.adapter(CurrentEnvironmentUpdateGet::class.java)

    suspend fun listen(context: Context, monitorDeviceUsername: String, onUpdate: suspend (CurrentEnvironmentUpdateGet) -> Unit) {
        val serverURL: String = SettingsDatastore(context).getWSSServerURL()
        val session = client.connect("${serverURL}ws")

        try {
            session
                .subscribeText("/topic/$monitorDeviceUsername/current")
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