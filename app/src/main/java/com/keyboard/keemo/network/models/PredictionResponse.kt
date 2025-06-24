package com.keyboard.keemo.network.models

data class PredictionResponse(
    @JvmField val predictions: List<Prediction>?,
    @JvmField val message: String?
) {
    data class Prediction(
        @JvmField val word: String,
        @JvmField val probability: Double
    )
}