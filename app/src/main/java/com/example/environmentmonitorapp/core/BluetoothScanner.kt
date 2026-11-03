package com.example.environmentmonitorapp.core

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanResult
import android.content.Context
import androidx.annotation.RequiresPermission
import com.espressif.provisioning.ESPProvisionManager
import com.espressif.provisioning.listeners.BleScanListener
import java.lang.Exception

class BluetoothScanner {
    companion object {
        fun scan(
            context: Context,
            prefix: String,
            onDeviceFound: (device: ProvisionDevice) -> Unit,
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

                            onDeviceFound(ProvisionDevice( bluetoothDevice = device, primaryServiceUuid = serviceUuid))
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
    }

}