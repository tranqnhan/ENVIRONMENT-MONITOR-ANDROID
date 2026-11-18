package com.example.environmentmonitorapp.core

object TransformURL {
    fun normalizeBaseURL(input: String, defaultScheme: String = "https"): String {
        val url = input.trim()
        require(url.isNotEmpty()) { "Server address cannot be empty" }
        require(defaultScheme == "http" || defaultScheme == "https" || defaultScheme == "wss")

        val withScheme = when {
            url.startsWith("wss://", ignoreCase = true) -> url
            url.startsWith("https://", ignoreCase = true) -> url
            url.startsWith("http://", ignoreCase = true) -> url
            else -> {
                require("://" !in url) { "Unsupported URL scheme" }
                "$defaultScheme://$url"
            }
        }

        return withScheme.trimEnd('/') + "/"
    }

}