package com.example.tolets.Model

data class DataRiwayat(
    val key: String = "",
    val tanggal: String = "",
    val temperature: Double = 0.0,
    val humidity: Double = 0.0,
    val airQuality: Int = 0,
    val keterangan: String = "",
    val lokasi: String = ""
)
