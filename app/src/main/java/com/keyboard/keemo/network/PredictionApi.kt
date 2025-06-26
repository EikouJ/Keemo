package com.keyboard.keemo.network

import com.keyboard.keemo.network.models.PredictionRequest
import com.keyboard.keemo.network.models.AutocompletionRequest
import com.keyboard.keemo.network.models.PredictionResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface PredictionApi {
    @POST("predict_next_word")
    fun getNextWordPrediction(@Body request: PredictionRequest): Call<PredictionResponse>

    @POST("autocompletion")
    fun getAutocompletion(@Body request: AutocompletionRequest): Call<PredictionResponse>

}