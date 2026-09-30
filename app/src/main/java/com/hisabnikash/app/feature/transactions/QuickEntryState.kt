package com.hisabnikash.app.feature.transactions

import com.hisabnikash.app.core.model.Category
import com.hisabnikash.app.core.model.PaymentMethod
import com.hisabnikash.app.core.model.Transaction
import com.hisabnikash.app.core.model.TransactionType

data class QuickEntryState(
    val type: TransactionType = TransactionType.EXPENSE,
    val amountInput: String = "",
    val description: String = "",
    val categoryId: String? = null,
    val categories: List<Category> = emptyList(),
    val customCategoryLabel: String = "",
    val saveCustomCategory: Boolean = false,
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val note: String = "",
    val occurredAtEpochMillis: Long = 0L,
    val isSaving: Boolean = false,
    val errors: Map<FormField, String> = emptyMap(),
    val saveError: String? = null,
)

enum class FormField { AMOUNT, DESCRIPTION, CATEGORY, CUSTOM_CATEGORY }

sealed interface SubmitResult {
    data class Saved(val transactionId: String) : SubmitResult
    data class PossibleDuplicate(val existing: Transaction) : SubmitResult
    data class ValidationFailed(val errors: Map<FormField, String>) : SubmitResult
    data class Failed(val message: String) : SubmitResult
}
