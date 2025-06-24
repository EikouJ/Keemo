package com.keyboard.keemo.prediction

import android.util.Log
import com.keyboard.keemo.network.ApiClient
import com.keyboard.keemo.network.models.PredictionRequest
import com.keyboard.keemo.network.models.PredictionResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PredictionManager {

    interface PredictionCallback {
        fun onPredictionsReceived(predictions: List<String>)
        fun onError(error: String)
    }

    private val api = ApiClient.predictionApi
    private val TAG = "PredictionManager"

    fun getPredictions(phrase: String, callback: PredictionCallback) {
        if (phrase.isBlank()) {
            callback.onPredictionsReceived(emptyList())
            return
        }

        val request = PredictionRequest(phrase = phrase.trim(), top_k = 3)

        api.getPredictions(request).enqueue(object : Callback<PredictionResponse> {
            override fun onResponse(call: Call<PredictionResponse>, response: Response<PredictionResponse>) {
                if (response.isSuccessful) {
                    val predictionResponse = response.body()
                    if (predictionResponse != null && predictionResponse.predictions != null) {
                        val words = predictionResponse.predictions.map { it.word }
                        Log.d(TAG, "Prédictions reçues: $words")
                        callback.onPredictionsReceived(words)
                    } else {
                        Log.w(TAG, "Réponse vide ou nulle")
                        callback.onPredictionsReceived(emptyList())
                    }
                } else {
                    val errorMsg = "Erreur HTTP: ${response.code()} - ${response.message()}"
                    Log.e(TAG, errorMsg)
                    callback.onError(errorMsg)
                }
            }

            override fun onFailure(call: Call<PredictionResponse>, t: Throwable) {
                val errorMsg = "Erreur réseau: ${t.message}"
                Log.e(TAG, errorMsg, t)
                callback.onError(errorMsg)
            }
        })
    }

    // Méthode pour nettoyer le texte d'entrée
    private fun cleanInput(text: String): String {
        return text.trim()
            .replace(Regex("[^\\w\\s]"), "") // Supprimer la ponctuation
            .replace(Regex("\\s+"), " ") // Normaliser les espaces
    }

    // Méthode pour obtenir les derniers mots pour la prédiction
    fun getLastWords(text: String, wordCount: Int = 2): String {
        val cleaned = cleanInput(text)
        val words = cleaned.split(" ").filter { it.isNotBlank() }
        return if (words.size >= wordCount) {
            words.takeLast(wordCount).joinToString(" ")
        } else {
            cleaned
        }
    }
}