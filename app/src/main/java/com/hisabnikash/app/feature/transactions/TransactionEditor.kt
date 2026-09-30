package com.hisabnikash.app.feature.transactions

import com.hisabnikash.app.core.model.Transaction

data class TransactionEditor(
    val original: Transaction,
    val draft: Transaction = original,
) {
    val canSave: Boolean get() = draft.amountMinor > 0 && draft.description.isNotBlank()
}
