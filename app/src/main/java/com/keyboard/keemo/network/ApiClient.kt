package com.keyboard.keemo.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    private const val NGRAM_BASE_URL = "https://keemo-api-predictions.onrender.com/"

    @JvmStatic
    val predictionApi: PredictionApi by lazy {
        Retrofit.Builder()
            .baseUrl(NGRAM_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PredictionApi::class.java)
    }
}