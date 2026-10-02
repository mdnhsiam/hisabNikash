package com.hisabnikash.app.backup.model

import kotlinx.serialization.Serializable

@Serializable
data class BackupEnvelope(
    val formatVersion: Int,
    val kdfIterations: Int,
    val saltBase64: String,
    val nonceBase64: String,
    val ciphertextBase64: String,
)
