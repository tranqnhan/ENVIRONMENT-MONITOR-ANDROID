package com.example.environmentmonitorapp.core

import android.content.Context
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import okio.ByteString.Companion.toByteString
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsDatastore(private val context: Context) {
    companion object {
        val SERVER_PASSWORD = stringPreferencesKey("encrypted_server_password")
        val SERVER_PASSWORD_IV = stringPreferencesKey("encrypted_server_password_iv")
        val SERVER_URL = stringPreferencesKey("server_url")
    }

    suspend fun saveServerCredentials(serverURL: String, serverPassword: String) {
        // encryption
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, EncryptionKey.getOrCreateKey())

        val cipherText = cipher.doFinal(serverPassword.toByteArray(Charsets.UTF_8))
        val iv = cipher.iv

        context.dataStore.updateData {
            it.toMutablePreferences().also { preferences ->
                preferences[SERVER_URL] = serverURL
                preferences[SERVER_PASSWORD] = Base64.encodeToString(cipherText, Base64.NO_WRAP)
                preferences[SERVER_PASSWORD_IV] = Base64.encodeToString(iv, Base64.NO_WRAP)
            }
        }
    }

    suspend fun getServerURL(): String? {
        val preferences = context.dataStore.data.first()

        return preferences[SERVER_URL]
    }


    suspend fun getServerPassword(): String? {
        val preferences = context.dataStore.data.first()

        val cipherText = Base64.decode(preferences[SERVER_PASSWORD], Base64.DEFAULT)
        val iv = Base64.decode(preferences[SERVER_PASSWORD_IV], Base64.DEFAULT)

        val cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING")
        cipher.init(
            Cipher.DECRYPT_MODE,
            EncryptionKey.getOrCreateKey(),
            IvParameterSpec(iv)
        )
        return Base64.encodeToString(cipher.doFinal(cipherText), Base64.NO_WRAP)
    }
}