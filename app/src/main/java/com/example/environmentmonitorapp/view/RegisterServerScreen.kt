package com.example.environmentmonitorapp.view

import android.content.Context
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.datastore.core.IOException
import com.example.environmentmonitorapp.api.ServerLogin
import kotlinx.coroutines.launch
import com.example.environmentmonitorapp.api.RetrofitClient
import com.example.environmentmonitorapp.core.SettingsDatastore

class RegisterServerScreen: InternalScreen() {
    init {
        screenName = "Register Server";
    }

    @Composable
    override fun Display() {
        var success by remember { mutableStateOf(false) }

        Column (
            modifier = Modifier
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Connect to a Server")
            if (success) {
                Text("Success: Server credentials registered.")
            } else {
                ServerLogin { success = true }
            }
        }
    }

    @Composable
    fun ServerLogin(onSuccess: () -> Unit) {
        var serverURL by remember { mutableStateOf("") };
        var serverPassword by remember { mutableStateOf("") }
        var showPassword by remember {mutableStateOf(false)}

        OutlinedTextField(
            value = serverURL,
            onValueChange = { serverURL = it },
            label = { Text("Server URL") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = serverPassword,
            onValueChange = { serverPassword = it },
            label = { Text("Server Password") },
            singleLine = true,
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password
            ),
            modifier = Modifier.fillMaxWidth()
        )


        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = showPassword,
                onCheckedChange = { showPassword = it }
            )

            Text("Show password")
        }

        val interactionSource = remember { MutableInteractionSource() }
        val scanButtonIsPressed by interactionSource.collectIsPressedAsState()
        val scanButtonColor = when {
            scanButtonIsPressed -> MaterialTheme.colorScheme.primaryContainer
            else -> MaterialTheme.colorScheme.primary
        }
        val scope = rememberCoroutineScope()
        var error by remember { mutableStateOf("") }
        var isLoading by remember { mutableStateOf(false) }

        val context: Context = LocalContext.current

        if (!isLoading) {
            Button(
                enabled = serverURL.isNotEmpty() && serverPassword.isNotEmpty(),
                onClick = {
                    isLoading = true

                    scope.launch {
                        try {
                            val response = RetrofitClient.getAPI(serverURL).authConnection(
                                ServerLogin(serverPassword = serverPassword)
                            )

                            if (response.isSuccessful) {
                                SettingsDatastore(context).saveServerCredentials(serverURL, serverPassword)
                                onSuccess()
                            } else {
                                error = "HTTPS error: " + response.code()
                            }
                        } catch (e: IOException) {
                            error = "Error: " + e.message.toString()
                        }

                        isLoading = false
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
                Text("REGISTER SERVER")
            }
        } else {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

    }

}