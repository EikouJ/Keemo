package com.keyboard.keemo.network.models

data class PredictionResponse(
    val predictions: List<Prediction>?,
    val message: String?
) {
    data class Prediction(
        val word: String,
        val probability: Double
    )
}