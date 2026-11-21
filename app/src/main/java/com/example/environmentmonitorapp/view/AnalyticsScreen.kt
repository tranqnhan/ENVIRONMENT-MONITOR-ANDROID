package com.example.environmentmonitorapp.view

import android.content.Context
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.example.environmentmonitorapp.api.RetrofitClient
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.environmentmonitorapp.api.EnvironmentUpdateClient
import com.example.environmentmonitorapp.api.EnvironmentDataPoint
import com.example.environmentmonitorapp.api.EnvironmentDataRange
import com.example.environmentmonitorapp.components.EnvironmentCard
import com.example.environmentmonitorapp.components.EnvironmentChart
import com.example.environmentmonitorapp.components.EnvironmentDataColor
import com.example.environmentmonitorapp.components.EnvironmentDataType
import com.example.environmentmonitorapp.components.HistoryRangeSelector
import com.example.environmentmonitorapp.core.SettingsDatastore
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.coroutines.cancellation.CancellationException

class AnalyticsScreen: InternalScreen() {
    init {
        screenName = "Analytics";
    }

    @Composable
    override fun Display() {
        val context = LocalContext.current
        var monitorDeviceUsernames by remember { mutableStateOf<List<String>>(emptyList()) }
        var selectedMonitorDeviceUsername by rememberSaveable { mutableStateOf("") }
        var isLoading by rememberSaveable { mutableStateOf(true) }
        var errorMessage by rememberSaveable {mutableStateOf<String?>(null)}
        var retryToggle by remember { mutableStateOf(false) }

        LaunchedEffect(retryToggle) {
            errorMessage = null
            isLoading = true

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
                val interactionSource = remember { MutableInteractionSource() }
                val buttonIsPressed by interactionSource.collectIsPressedAsState()
                val buttonColor = when {
                    buttonIsPressed -> MaterialTheme.colorScheme.primaryContainer
                    else -> MaterialTheme.colorScheme.primary
                }

                Button(
                    onClick = { retryToggle = !retryToggle },
                    interactionSource = interactionSource,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RectangleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = buttonColor,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("RETRY CONNECTION")
                }
                return
            }

            if (selectedMonitorDeviceUsername.isNotBlank()) {
                LoadAnalytics(context, selectedMonitorDeviceUsername)
                return
            }

            if (monitorDeviceUsernames.isNotEmpty()) {
                MonitorDeviceSelection(
                    monitorDeviceUsernames = monitorDeviceUsernames,
                    onMonitorDeviceSelect = {
                        selectedMonitorDeviceUsername = it
                        monitorDeviceUsernames = emptyList()
                    }
                )
                return
            }

            Text("No devices found. Add a new device in Register Device.")
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
    fun LoadAnalytics(context: Context, monitorDeviceUsername: String) {
        val client = remember { EnvironmentUpdateClient() }
        var currentData by remember { mutableStateOf<EnvironmentDataPoint?>(null) }
        var dataHistory by remember { mutableStateOf<List<EnvironmentDataPoint>>(emptyList())}
        var errorMessage by remember { mutableStateOf<String?>(null) }
        var isConnect by remember { mutableStateOf(false) }
        var retryToggle by remember { mutableStateOf(false) }

        LaunchedEffect(retryToggle) {
            errorMessage = null
            isConnect = false

            try {
                val environmentDataResponse =
                    RetrofitClient.getAPI(context).getCurrentEnvironmentData(monitorDeviceUsername)

                if (environmentDataResponse.isSuccessful) {
                    currentData = environmentDataResponse.body()
                }

                coroutineScope {
                    launch {
                        client.listen(context, "current", monitorDeviceUsername) { update ->
                            currentData = update
                            isConnect = true
                        }
                    }

                    launch {
                        client.listen(context, "history", monitorDeviceUsername) { update ->
                            dataHistory = dataHistory + update
                        }
                    }
                }
            } catch (e: CancellationException) {
                isConnect = false
                throw e
            } catch (e: Exception) {
                errorMessage = e.message ?: "Connection failed"
                isConnect = false
            }
        }

        var selectedDays by rememberSaveable {
            mutableStateOf<Int?>(7)
        }

        LaunchedEffect(selectedDays) {
            val end = Instant.now()

            val start = selectedDays?.let { days ->
                end.minus(days.toLong(), ChronoUnit.DAYS)
            } ?: Instant.EPOCH

            val environmentDataHistoryResponse =
                RetrofitClient.getAPI(context).getEnvironmentDataHistory(
                    EnvironmentDataRange(
                        monitorDeviceUsername,
                        start,
                        end
                    )
                )

            if (environmentDataHistoryResponse.isSuccessful) {
                dataHistory = environmentDataHistoryResponse.body()!!
            }
        }

        Column {
            errorMessage?.let { message ->
                Text("Failed: $message")

                val interactionSource = remember { MutableInteractionSource() }
                val buttonIsPressed by interactionSource.collectIsPressedAsState()
                val buttonColor = when {
                    buttonIsPressed -> MaterialTheme.colorScheme.primaryContainer
                    else -> MaterialTheme.colorScheme.primary
                }

                Button(
                    onClick = { retryToggle = !retryToggle },
                    interactionSource = interactionSource,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RectangleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = buttonColor,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("RETRY CONNECTION")
                }
            }

            currentData?.let { update ->
                EnvironmentCard(
                    monitorDeviceUsername,
                    update,
                    isConnect,
                )
            }?: run {
                if (errorMessage == null) {
                    Text("Loading live data for device $monitorDeviceUsername")
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }

            if (dataHistory.isNotEmpty()) {
                HistoryRangeSelector(
                    selectedDays,
                    onDaysSelected = { selectedDays = it }
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Temperature",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        color = EnvironmentDataColor[EnvironmentDataType.TEMPERATURE]!!
                    )
                    EnvironmentChart(context, dataHistory, EnvironmentDataType.TEMPERATURE)

                    Text(
                        text = "Humidity",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        color = EnvironmentDataColor[EnvironmentDataType.HUMIDITY]!!
                    )
                    EnvironmentChart(context, dataHistory, EnvironmentDataType.HUMIDITY)


                    Text(
                        text = "CO₂",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        color = EnvironmentDataColor[EnvironmentDataType.CO2]!!
                    )
                    EnvironmentChart(context, dataHistory, EnvironmentDataType.CO2)
                }
            }
        }
    }
}