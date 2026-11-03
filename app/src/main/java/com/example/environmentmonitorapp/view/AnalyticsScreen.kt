package com.example.environmentmonitorapp.view

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable


class AnalyticsScreen: InternalScreen() {
    init {
        screenName = "Analytics";
    }

    @Composable
    override fun Display() {
        Text(screenName)
    }
}