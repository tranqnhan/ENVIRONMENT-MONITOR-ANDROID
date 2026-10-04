package com.example.environmentmonitorapp

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.espressif.provisioning.ESPConstants
import com.espressif.provisioning.ESPDevice
import com.espressif.provisioning.ESPProvisionManager
import com.espressif.provisioning.listeners.BleScanListener
import com.espressif.provisioning.listeners.ProvisionListener
import java.lang.Exception

data class ProvisioningDevice (
    val bluetoothDevice: BluetoothDevice,
    val primaryServiceUuid: String
)

data class ProvisioningResult (
    val status: Boolean,
    val message: String
)

class SettingsScreen: InternalScreen() {
    init {
        screenName = "Settings";
    }

    fun beginBluetoothScan(
        context: Context,
        prefix: String,
        onDeviceFound: (device: ProvisioningDevice) -> Unit,
        onScanComplete: () -> Unit
    ) {
        // Start searching BLE ESP devices with prefix
        ESPProvisionManager.getInstance(context)
            .searchBleEspDevices(
                prefix,
                object : BleScanListener {
                    override fun scanStartFailed() {
                        TODO("Not yet implemented")
                    }

                    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
                    override fun onPeripheralFound(
                        device: BluetoothDevice?,
                        result: ScanResult?
                    ) {
                        if (device == null || result == null) return

                        val serviceUuid = result.scanRecord
                            ?.serviceUuids
                            ?.firstOrNull()
                            ?.uuid
                            ?.toString()
                            ?: return

                        onDeviceFound(ProvisioningDevice(
                            bluetoothDevice = device,
                            primaryServiceUuid = serviceUuid
                        ))
                    }

                    override fun scanCompleted() {
                        onScanComplete()
                    }

                    override fun onFailure(p0: Exception?) {
                        TODO("Not yet implemented")
                    }
                }
            )
    }

    fun connectESPDevice(context: Context, provisioningDevice: ProvisioningDevice): ESPDevice {
        val espDevice = ESPProvisionManager.getInstance(context)
            .createESPDevice(
                ESPConstants.TransportType.TRANSPORT_BLE,
                ESPConstants.SecurityType.SECURITY_2
            )

        espDevice.connectBLEDevice(
            provisioningDevice.bluetoothDevice,
            provisioningDevice.primaryServiceUuid
        )

        //TODO: set proof of possession
        espDevice.userName = "BLE_ESP32_PROV"
        espDevice.proofOfPossession = "12345678"

        return espDevice;
    }

    fun provisionDeviceWifi(
        espDevice: ESPDevice,
        ssid: String,
        password: String,
        onProvisionComplete: (result: ProvisioningResult) -> Unit
    ) {
        espDevice.provision(ssid,password,
            object: ProvisionListener {
                override fun createSessionFailed(p0: Exception?) {
                    onProvisionComplete(
                        ProvisioningResult(
                            false, "Failure: Create session failed"
                        )
                    )
                }

                override fun wifiConfigSent() {

                }

                override fun wifiConfigFailed(p0: Exception?) {
                    onProvisionComplete(
                    ProvisioningResult(
                        false,
                        "Failure: Wi-Fi configurations failed" + p0?.let{ ". Reason: " + p0.message }
                        )
                    )
                }

                override fun wifiConfigApplied() {

                }

                override fun wifiConfigApplyFailed(p0: Exception?) {
                    onProvisionComplete(
                        ProvisioningResult(
                            false,
                            "Failure: Wi-Fi apply configurations failed" + p0?.let{ ". Reason: " + p0.message }
                        )
                    )
                }

                override fun provisioningFailedFromDevice(p0: ESPConstants.ProvisionFailureReason?) {
                    onProvisionComplete(
                        ProvisioningResult(
                            false,
                            "Failure: Device failure" + p0?.let{ ". Reason: $p0" }
                        )
                    )
                }

                override fun deviceProvisioningSuccess() {
                    onProvisionComplete(
                        ProvisioningResult(
                            true, "Success: Device successfully provisioned"
                        )
                    )
                }

                override fun onProvisioningFailed(p0: Exception?) {
                    onProvisionComplete(
                    ProvisioningResult(
                            false,
                            "Failure: Provisioning failure" + p0?.let{ ". Reason: $p0" }
                        )
                    )
                }

            }
        )
    }




    @Composable
    fun BluetoothPermissionCheck(onPermissionGranted: (result: Boolean) -> Unit) {
        val context = LocalContext.current

        // Checking for permissions
        val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val scanGranted = permissions[Manifest.permission.BLUETOOTH_SCAN] == true
                val connectGranted = permissions[Manifest.permission.BLUETOOTH_CONNECT] == true
                onPermissionGranted(scanGranted && connectGranted)
            } else {
                val accessFineLocationGranted =
                    permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
                onPermissionGranted(accessFineLocationGranted)
            }
        }

        LaunchedEffect(Unit) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val scanGranted =
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.BLUETOOTH_SCAN
                    ) == PackageManager.PERMISSION_GRANTED

                val connectGranted =
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) == PackageManager.PERMISSION_GRANTED

                if (scanGranted && connectGranted) {
                    onPermissionGranted(true)
                } else {
                    bluetoothPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.BLUETOOTH_SCAN,
                            Manifest.permission.BLUETOOTH_CONNECT
                        )
                    )
                }
            } else {
                val accessFineLocationGranted =
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                if (accessFineLocationGranted) {
                    onPermissionGranted(true)
                } else {
                    bluetoothPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION
                        )
                    )
                }
            }
        }
    }

    @Composable
    fun BluetoothDeviceList(provisioningDevices: List<ProvisioningDevice>, onGetESPDevice: (espDevice: ESPDevice) -> Unit) {
        val context = LocalContext.current

        if (ContextCompat.checkSelfPermission(context,Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) return

        LazyColumn (
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(provisioningDevices) { provisioningDevice ->
                OutlinedButton(
                    onClick = {
                        val espDevice = connectESPDevice(context, provisioningDevice)
                        onGetESPDevice(espDevice)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RectangleShape,
                    border = BorderStroke(1.dp, Color.Cyan)
                ) {
                    Text(provisioningDevice.bluetoothDevice.name ?: "Unknown device")
                }
            }
        }
    }

    @Composable
    fun BluetoothScan(onESPDeviceReceived: (ESPDevice) -> Unit) {
        val context = LocalContext.current

        // BLUETOOTH TEXT FIELD
        var scanState by remember { mutableStateOf<BluetoothScanState>(BluetoothScanState.IDLE) }
        var provisioningDeviceName by remember { mutableStateOf("") }
        val provisioningDevices = remember { mutableStateListOf<ProvisioningDevice>() }

        OutlinedTextField(
            value = provisioningDeviceName,
            onValueChange = {
                provisioningDeviceName = it
                scanState = BluetoothScanState.IDLE
            },
            label = { Text("Bluetooth Device Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        // SCAN BUTTON
        val interactionSource = remember { MutableInteractionSource() }
        val scanButtonIsPressed by interactionSource.collectIsPressedAsState()
        val scanButtonColor = when {
            scanButtonIsPressed -> MaterialTheme.colorScheme.primaryContainer
            else -> MaterialTheme.colorScheme.primary
        }

        Button(
            enabled = provisioningDeviceName.isNotEmpty() && scanState == BluetoothScanState.IDLE,
            onClick = {
                scanState = BluetoothScanState.SCANNING

                provisioningDevices.clear()

                beginBluetoothScan(
                    context = context,
                    prefix = provisioningDeviceName,
                    onScanComplete = {
                        scanState = BluetoothScanState.COMPLETE
                    },
                    onDeviceFound = { provisioningDevice ->
                        if (!provisioningDevices.any {
                                it.bluetoothDevice.address == provisioningDevice.bluetoothDevice.address
                            }) {
                            provisioningDevices.add(provisioningDevice)
                        }
                    }
                )

            },
            interactionSource = interactionSource,
            modifier = Modifier.fillMaxWidth(),
            shape = RectangleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = scanButtonColor,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text("SCAN")
        }
        
        if (scanState != BluetoothScanState.IDLE) {
            BluetoothDeviceList(
                provisioningDevices,
                onGetESPDevice = {
                    espDevice ->
                        onESPDeviceReceived(espDevice)
                }
            )
        }
    }

    @Composable
    fun WifiProvision(espDevice: ESPDevice) {
        var ssid by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }

        OutlinedTextField(
            value = ssid,
            onValueChange = {
                ssid = it
            },
            label = { Text("Wi-Fi SSID") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
            },
            label = { Text("Password") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )


        val interactionSource = remember { MutableInteractionSource() }
        val scanButtonIsPressed by interactionSource.collectIsPressedAsState()
        val scanButtonColor = when {
            scanButtonIsPressed -> MaterialTheme.colorScheme.primaryContainer
            else -> MaterialTheme.colorScheme.primary
        }

        var statusText by remember { mutableStateOf("") }
        var isLoading by remember {mutableStateOf(false)}

        if (!isLoading) {
            Button(
                enabled = ssid.isNotEmpty(),
                onClick = {
                    isLoading = true
                    provisionDeviceWifi(
                        espDevice,
                        ssid,
                        password,
                        onProvisionComplete = { result ->
                            statusText += "\n" + result.message
                            isLoading = false
                        })
                },
                interactionSource = interactionSource,
                modifier = Modifier.fillMaxWidth(),
                shape = RectangleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = scanButtonColor,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("SUBMIT")
            }

        } else {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        Text(
            text = statusText,
            modifier = Modifier
                .fillMaxWidth()

        )

    }


    @Composable
    fun BluetoothScanAndWifiProvision() {
        val context: Context = LocalContext.current

        var permissionGranted by remember {
            mutableStateOf(false)
        }

        BluetoothPermissionCheck(
            onPermissionGranted = {
                permissionGranted = it
            }
        )

        if (!permissionGranted) return

        var espDevice: ESPDevice? by remember { mutableStateOf(null)}

        if (espDevice == null) {
            BluetoothScan(
                onESPDeviceReceived = {
                    espDevice = it
                }
            )
        } else {
            WifiProvision(espDevice!!)
        }
    }

    @Composable
    override fun Display() {
        Column (
            modifier = Modifier
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Network Provisioning")

            BluetoothScanAndWifiProvision()
        }

    }
}

