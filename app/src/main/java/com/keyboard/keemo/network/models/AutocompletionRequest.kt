package com.keyboard.keemo.network.models

data class AutocompletionRequest(
    @JvmField val prefix: String,
    @JvmField val top_k: Int = 3
)
