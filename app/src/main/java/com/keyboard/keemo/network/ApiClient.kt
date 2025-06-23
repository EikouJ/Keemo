package com.keyboard.keemo.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    // Changer cette URL pour la production
    private const val BASE_URL = "http://10.0.2.2:8000" // Émulateur

    val predictionApi: PredictionApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PredictionApi::class.java)
    }
}