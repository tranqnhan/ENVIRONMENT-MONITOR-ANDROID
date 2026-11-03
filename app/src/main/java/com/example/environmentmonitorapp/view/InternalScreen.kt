package com.example.environmentmonitorapp.view

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

open class InternalScreen {
    protected var screenName: String = "";

    fun getName(): String {
        return screenName
    }

    @Composable
    open fun Display() {

    }
}

