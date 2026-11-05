package com.example.environmentmonitorapp.core

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
val SERVER_PASSWORD = stringPreferencesKey("encrypted_server_password")
val SERVER_PASSWORD_IV = stringPreferencesKey("encrypted_server_password_iv")
val SERVER_URL = stringPreferencesKey("server_url")

