package com.keyboard.keemo.network.models

data class PredictionRequest(
    val phrase: String,
    val top_k: Int = 3
)