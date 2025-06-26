package com.keyboard.keemo.prediction

import android.util.Log
import com.keyboard.keemo.network.ApiClient
import com.keyboard.keemo.network.models.AutocompletionRequest
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

    enum class PredictionType {
        NGRAM,      // Prédiction basée sur les phrases
        LETTERS     // Prédiction basée sur les lettres
    }

    private val api = ApiClient.predictionApi
    private val TAG = "PredictionManager"

    fun getNextWordPrediction(phrase: String, topK: Int = 3, callback: PredictionCallback) {
        val cleanedPhrase = cleanInput(phrase)
        if (cleanedPhrase.isBlank()) {
            callback.onError("Phrase vide")
            return
        }

        val request = PredictionRequest(phrase = cleanedPhrase, top_k = topK)
        Log.d(TAG, "Demande de prédiction de mot (phrase): $cleanedPhrase")

        api.getNextWordPrediction(request).enqueue(getCallback(callback, "next_word"))
    }

    fun getAutocompletion(currentWord: String, topK: Int = 3, callback: PredictionCallback) {
        val cleanedWord = currentWord.trim()
        if (cleanedWord.isBlank()) {
            callback.onPredictionsReceived(emptyList())
            return
        }

        val request = AutocompletionRequest(prefix = cleanedWord, top_k = topK)
        Log.d(TAG, "Demande d'autocomplétion (prefix): $cleanedWord")

        api.getAutocompletion(request).enqueue(getCallback(callback, "autocompletion"))
    }

    private fun getCallback(callback: PredictionCallback, requestType: String): Callback<PredictionResponse> {
        return object : Callback<PredictionResponse> {
            override fun onResponse(call: Call<PredictionResponse>, response: Response<PredictionResponse>) {
                try {
                    if (response.isSuccessful) {
                        val predictions = response.body()?.predictions
                        if (!predictions.isNullOrEmpty()) {
                            val words = predictions.map { it.word }
                            Log.d(TAG, "Prédictions reçues pour $requestType: $words")
                            callback.onPredictionsReceived(words)
                        } else {
                            Log.d(TAG, "Aucune prédiction trouvée pour $requestType")
                            callback.onPredictionsReceived(emptyList())
                        }
                    } else {
                        val errorMsg = "Erreur HTTP : ${response.code()} - ${response.message()}"
                        Log.e(TAG, "$errorMsg pour $requestType")
                        callback.onError(errorMsg)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Erreur lors du traitement de la réponse $requestType", e)
                    callback.onError("Erreur de traitement : ${e.message}")
                }
            }

            override fun onFailure(call: Call<PredictionResponse>, t: Throwable) {
                val errorMsg = "Erreur réseau pour $requestType: ${t.message}"
                Log.e(TAG, errorMsg, t)
                callback.onError(errorMsg)
            }
        }
    }

    private fun cleanInput(text: String): String {
        return text.trim()
            .replace(Regex("[^\\p{L}\\p{N}\\s]"), "")  // Garde lettres, chiffres, espaces
            .replace(Regex("\\s+"), " ")  // Normalise les espaces
    }

    fun getLastWords(text: String, wordCount: Int = 2): String {
        val cleaned = cleanInput(text)
        val words = cleaned.split(" ").filter { it.isNotBlank() }
        return if (words.size >= wordCount) {
            words.takeLast(wordCount).joinToString(" ")
        } else {
            cleaned
        }
    }

    fun getCurrentWord(text: String): String {
        val words = text.split(Regex("\\s+"))
        return words.lastOrNull()?.trim()?.replace(Regex("[^\\p{L}\\p{N}]$"), "") ?: ""
    }

    fun getPreviousContext(text: String): String {
        val words = cleanInput(text).split(" ").filter { it.isNotBlank() }
        return if (words.size > 1) {
            words.dropLast(1).takeLast(2).joinToString(" ")
        } else {
            ""
        }
    }

    fun shouldUseAutocompletion(text: String): Boolean {
        val currentWord = getCurrentWord(text)
        return currentWord.isNotBlank() && !text.endsWith(" ")
    }
}
