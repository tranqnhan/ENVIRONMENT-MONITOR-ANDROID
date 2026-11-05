package com.example.environmentmonitorapp.view

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.espressif.provisioning.ESPDevice
import com.example.environmentmonitorapp.core.BluetoothScanState
import com.example.environmentmonitorapp.core.BluetoothScanner
import com.example.environmentmonitorapp.core.ProvisionDevice
import com.example.environmentmonitorapp.core.VerifyDeviceState

class RegisterDeviceScreen: InternalScreen() {
    init {
        screenName = "Register Device";
    }

    @Composable
    override fun Display() {
        Column (
            modifier = Modifier
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Register a monitor device")

            var permissionGranted by remember {
                mutableStateOf(false)
            }

            BluetoothPermissionCheck(
                onPermissionGranted = {
                    permissionGranted = it
                }
            )

            if (!permissionGranted) return

            var provisionDevice: ProvisionDevice? by remember { mutableStateOf(null)}

            if (provisionDevice == null) {
                BluetoothScan( onReceiveProvisionDevice = { provisionDevice = it; })
            } else {
                WifiProvision(provisionDevice!!)
            }
        }

    }

    @Composable
    fun BluetoothScan(onReceiveProvisionDevice: (ProvisionDevice) -> Unit) {
        val context = LocalContext.current

        // BLUETOOTH TEXT FIELD
        var scanState by remember { mutableStateOf<BluetoothScanState>(BluetoothScanState.IDLE) }
        var provisionDeviceName by remember { mutableStateOf("") }
        val provisioningDevices = remember { mutableStateListOf<ProvisionDevice>() }
        var selectedProvisionDevice: ProvisionDevice? by remember { mutableStateOf(null) }

        if (selectedProvisionDevice == null) {
            OutlinedTextField(
                value = provisionDeviceName,
                onValueChange = {
                    provisionDeviceName = it
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
                enabled = provisionDeviceName.isNotEmpty() && scanState == BluetoothScanState.IDLE,
                onClick = {
                    scanState = BluetoothScanState.SCANNING

                    provisioningDevices.clear()

                    BluetoothScanner.scan(
                        context = context,
                        prefix = provisionDeviceName,
                        onScanComplete = {
                            scanState = BluetoothScanState.COMPLETE
                        },
                        onDeviceFound = { provisionDevice ->
                            if (!provisioningDevices.any {
                                    it.bluetoothDevice.address == provisionDevice.bluetoothDevice.address
                                }) {
                                provisioningDevices.add(provisionDevice)
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
                Text("SEARCH FOR DEVICE")
            }

            if (scanState != BluetoothScanState.IDLE) {
                BluetoothDeviceList(
                    provisioningDevices,
                    onSelectProvisioningDevice = {
                        selectedProvisionDevice = it
                    }
                )
            }
        } else {
            BluetoothVerifyDevice(selectedProvisionDevice!!, onReceiveProvisionDevice)
        }

    }

    @Composable
    fun BluetoothVerifyDevice(provisionDevice: ProvisionDevice, onReceiveProvisionDevice: (provisionDevice: ProvisionDevice) -> Unit) {
        val context = LocalContext.current

        if (ContextCompat.checkSelfPermission(context,Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) return

        Text("Device Name: " + provisionDevice.bluetoothDevice.name)

        var proofOfPossession by remember { mutableStateOf("") }

        OutlinedTextField(
            value = proofOfPossession,
            onValueChange = {
                proofOfPossession = it
            },
            label = { Text("PoP Code") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        val interactionSource = remember { MutableInteractionSource() }
        val scanButtonIsPressed by interactionSource.collectIsPressedAsState()
        val scanButtonColor = when {
            scanButtonIsPressed -> MaterialTheme.colorScheme.primaryContainer
            else -> MaterialTheme.colorScheme.primary
        }
        var isLoading by remember { mutableStateOf(false) }
        var infoMessage by remember { mutableStateOf("") }
        var retry: Boolean by remember { mutableStateOf(false) }

        if (!isLoading) {
            Button(
                enabled = proofOfPossession.isNotEmpty(),
                onClick = {
                    isLoading = true
                    infoMessage = ""

                    if (!retry) {
                        provisionDevice.connect(
                            context = context,
                            proofOfPossession = proofOfPossession,
                        )

                        provisionDevice.verify (
                            onDeviceVerify = { state: VerifyDeviceState, message: String ->
                                infoMessage = message
                                isLoading = false

                                when (state) {
                                    VerifyDeviceState.VALID -> {
                                        onReceiveProvisionDevice(provisionDevice)
                                    }
                                    VerifyDeviceState.TIMEOUT -> {
                                        retry = true
                                    }
                                    VerifyDeviceState.INVALID  -> {
                                        retry = false
                                    }
                                }
                            }
                        )
                    }
                },
                interactionSource = interactionSource,
                modifier = Modifier.fillMaxWidth(),
                shape = RectangleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = scanButtonColor,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(text = if (retry) "RETRY" else "SUBMIT")
            }

            Text(infoMessage)
        } else {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }

    @Composable
    fun BluetoothDeviceList(provisionDevices: List<ProvisionDevice>, onSelectProvisioningDevice: (selectedProvisioningDevice: ProvisionDevice) -> Unit) {
        val context = LocalContext.current

        if (ContextCompat.checkSelfPermission(context,Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) return

        LazyColumn (
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(provisionDevices) { provisionDevice ->
                OutlinedButton(
                    onClick = {
                        onSelectProvisioningDevice(provisionDevice)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RectangleShape,
                    border = BorderStroke(1.dp, Color.Cyan)
                ) {
                    Text(provisionDevice.bluetoothDevice.name ?: "Unknown device")
                }
            }
        }
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
    fun WifiProvision(provisionDevice: ProvisionDevice) {
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
                    provisionDevice.provisionWifi(
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

}

