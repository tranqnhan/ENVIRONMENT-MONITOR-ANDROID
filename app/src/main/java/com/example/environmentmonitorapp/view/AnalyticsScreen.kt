package com.example.environmentmonitorapp.view

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.example.environmentmonitorapp.api.RetrofitClient
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import com.example.environmentmonitorapp.api.CurrentEnvironmentUpdateClient
import com.example.environmentmonitorapp.api.CurrentEnvironmentUpdateGet
import com.example.environmentmonitorapp.components.EnvironmentCard
import com.example.environmentmonitorapp.core.SettingsDatastore
import kotlin.coroutines.cancellation.CancellationException

class AnalyticsScreen: InternalScreen() {
    init {
        screenName = "Analytics";
    }

    @Composable
    override fun Display() {
        val context = LocalContext.current
        var monitorDeviceUsernames by remember { mutableStateOf<List<String>>(emptyList()) }
        var selectedMonitorDeviceUsername by remember { mutableStateOf("") }
        var isLoading by remember { mutableStateOf(true) }
        var errorMessage by remember {mutableStateOf<String?>(null)}

        LaunchedEffect(Unit) {
            if (!SettingsDatastore(context).isServerURLExist()) {
                errorMessage = "No server registered"
            } else {
                try {
                    val monitorDeviceUsernamesResponse = RetrofitClient.getAPI(context).getAllMonitorDeviceUsernames()
                    if (monitorDeviceUsernamesResponse.isSuccessful) {
                        monitorDeviceUsernames = monitorDeviceUsernamesResponse.body()!!
                    }
                } catch (e: Exception) {
                    errorMessage = e.toString()
                }

            }

            isLoading = false
        }
        Column (
            modifier = Modifier
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
                return
            }

            errorMessage?.let {
                Text("Failure: $it.")
                return
            }

            if (selectedMonitorDeviceUsername.isNotBlank()) {
                LoadAnalytics(selectedMonitorDeviceUsername)
                return
            }

            if (monitorDeviceUsernames.isNotEmpty()) {
                MonitorDeviceSelection(
                    monitorDeviceUsernames = monitorDeviceUsernames,
                    onMonitorDeviceSelect = { selectedMonitorDeviceUsername = it }
                )
                return
            }

            Text("No devices found.")
        }
    }

    @Composable
    fun MonitorDeviceSelection(monitorDeviceUsernames: List<String>, onMonitorDeviceSelect: (monitorDeviceUsername: String) -> Unit) {
        LazyColumn (
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(monitorDeviceUsernames) { monitorDeviceUsername ->
                OutlinedButton(
                    onClick = {
                        onMonitorDeviceSelect(monitorDeviceUsername)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RectangleShape,
                    border = BorderStroke(1.dp, Color.Cyan)
                ) {
                    Text(monitorDeviceUsername)
                }
            }
        }
    }

    @Composable
    fun LoadAnalytics(monitorDeviceUsername: String) {
        val context = LocalContext.current
        val client = remember { CurrentEnvironmentUpdateClient() }

        var currentData by remember {
            mutableStateOf<CurrentEnvironmentUpdateGet?>(null)
        }

        var errorMessage by remember {
            mutableStateOf<String?>(null)
        }

        LaunchedEffect(Unit) {
            errorMessage = null

            try {
                client.listen(context, monitorDeviceUsername) { update ->
                    currentData = update
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                errorMessage = e.message ?: "Connection failed"
            }
        }

        Column {
            currentData?.let { update ->
                EnvironmentCard(
                    monitorDeviceUsername,
                    update
                )
            }?: run {
                Text("Loading live data for device $monitorDeviceUsername")
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            errorMessage?.let { message ->
                Text(message)
            }
        }
    }
}