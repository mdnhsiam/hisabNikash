package com.hisabnikash.app.data.repository

import com.hisabnikash.app.core.model.PaymentMethod

interface PaymentMethodPreference {
    suspend fun get(): PaymentMethod
    suspend fun set(method: PaymentMethod)
}

class InMemoryPaymentMethodPreference(
    initial: PaymentMethod = PaymentMethod.CASH,
) : PaymentMethodPreference {
    private var value = initial

    override suspend fun get(): PaymentMethod = value

    override suspend fun set(method: PaymentMethod) {
        value = method
    }
}
