package com.example.environmentmonitorapp.core

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresPermission
import com.espressif.provisioning.ESPConstants
import com.espressif.provisioning.ESPDevice
import com.espressif.provisioning.ESPProvisionManager
import com.espressif.provisioning.listeners.ProvisionListener
import com.espressif.provisioning.listeners.ResponseListener
import java.lang.Exception


/*
    STAGES:
    1. connect
    2. verify
    3. provisionWifi
*/

class ProvisionDevice(val bluetoothDevice: BluetoothDevice, val primaryServiceUuid: String) {
    var monitorDevice: ESPDevice? = null;

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun connect(context: Context, proofOfPossession: String) {
        this.monitorDevice = ESPProvisionManager.getInstance(context)
            .createESPDevice(
                ESPConstants.TransportType.TRANSPORT_BLE,
                ESPConstants.SecurityType.SECURITY_2
            )

        this.monitorDevice?.connectBLEDevice(this.bluetoothDevice, this.primaryServiceUuid)

        this.monitorDevice?.userName = this.bluetoothDevice.name
        this.monitorDevice?.proofOfPossession = proofOfPossession
    }

    fun verify(onSuccess: (message: String) -> Unit, onFailure: (message: String) -> Unit) {
        if (this.monitorDevice == null) return

        val handler = Handler(Looper.getMainLooper())
        val startTime = System.currentTimeMillis()
        val timeout = 10_000L

        val checkVersionInfo = object : Runnable {
            override fun run() {
                if (monitorDevice!!.versionInfo != null) {
                    Log.d("ESP", "versionInfo is ready")

                    monitorDevice!!.initSession(object : ResponseListener {
                        override fun onSuccess(returnData: ByteArray?) {
                            Log.d("ESP", "PoP/session SUCCESS")

                            onSuccess( "Success: PoP verified")
                        }

                        override fun onFailure(e: Exception) {
                            Log.e("ESP", "PoP/session FAILED", e)
                            onFailure("Failure: PoP failed to be verified")
                        }
                    })
                } else if (System.currentTimeMillis() - startTime < timeout) {
                    handler.postDelayed(this, 100)
                } else {
                    Log.e("ESP", "Timed out waiting for versionInfo")
                    onFailure("Failure: Timed out waiting for versionInfo. Please retry.")
                }
            }
        }

        handler.post(checkVersionInfo)
    }

    fun provisionServerUrl(
        serverUrl: String,
        onSuccess: () -> Unit,
        onFailure: (message: String) -> Unit
    ) {
        this.monitorDevice?.sendDataToCustomEndPoint(
            "server-url",
            serverUrl.toByteArray(Charsets.UTF_8),
            object : ResponseListener {
                override fun onSuccess(returnData: ByteArray) {
                    val reply = String(returnData, Charsets.UTF_8)
                    Log.e("REPLY", reply);
                    if (reply == "OK") {
                        onSuccess()
                    } else {
                        onFailure("Device did not acknowledge the server URL")
                    }
                }

                override fun onFailure(e: Exception) {
                    onFailure(e.toString())
                }
            }
        )
    }

    fun provisionWifi(
        ssid: String,
        password: String,
        onSuccess: () -> Unit,
        onFailure: (message: String) -> Unit
    ) {
        this.monitorDevice?.provision(ssid,password,
            object: ProvisionListener {
                override fun createSessionFailed(p0: Exception?) {
                    onFailure("Create session failed" + p0?.let{ ". Reason: ${p0.message}"})
                }

                override fun wifiConfigSent() {

                }

                override fun wifiConfigFailed(p0: Exception?) {
                    onFailure("Wi-Fi configurations failed" + p0?.let { ". Reason: ${p0.message}"})
                }

                override fun wifiConfigApplied() {

                }

                override fun wifiConfigApplyFailed(p0: Exception?) {
                    onFailure("Wi-Fi apply configurations failed" + p0?.let { ". Reason: ${p0.message}" })
                }

                override fun provisioningFailedFromDevice(p0: ESPConstants.ProvisionFailureReason?) {
                    onFailure("Device failure" + p0?.let { ". Reason: $p0" })
                }

                override fun deviceProvisioningSuccess() {
                    if (monitorDevice?.versionInfo == null) {
                        Log.d("VERSION INFO", "version info is null")
                    } else {
                        Log.d("VERSION INFO", monitorDevice!!.versionInfo)
                    }

                    onSuccess()
                }

                override fun onProvisioningFailed(p0: Exception?) {
                    onFailure("Provisioning failure" + p0?.let { ". Reason: ${p0.printStackTrace()}" })
                }

            }
        )
    }


    fun getUsername(): String {
        return monitorDevice!!.deviceName
    }

    fun getPassword(): String {
        return monitorDevice!!.proofOfPossession
    }

}