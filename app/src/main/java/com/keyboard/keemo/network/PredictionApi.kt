package com.keyboard.keemo.network

import com.keyboard.keemo.network.models.PredictionRequest
import com.keyboard.keemo.network.models.PredictionResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface PredictionApi {
    @POST("/predict")
    suspend fun getPredictions(
        @Body request: PredictionRequest
    ): Response<PredictionResponse>
}