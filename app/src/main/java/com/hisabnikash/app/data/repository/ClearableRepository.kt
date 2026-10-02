package com.hisabnikash.app.data.repository

interface ClearableRepository {
    suspend fun clearAll()
}
