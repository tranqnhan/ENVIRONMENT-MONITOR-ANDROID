package com.example.environmentmonitorapp.api

import com.squareup.moshi.FromJson
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.time.Instant

class InstantAdapter {
    @FromJson
    fun fromJson(value: String): Instant =
        Instant.parse(value)

    @ToJson
    fun toJson(value: Instant): String =
        value.toString()
}

object MoshiAdapterObject {
    val moshi: Moshi = Moshi.Builder()
        .add(InstantAdapter())
        .addLast(KotlinJsonAdapterFactory())
        .build()
}