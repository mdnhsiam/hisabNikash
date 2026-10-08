package com.hisabnikash.app.feature.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hisabnikash.app.core.model.Category
import com.hisabnikash.app.core.model.CategoryType
import com.hisabnikash.app.core.model.PaymentMethod
import com.hisabnikash.app.core.model.Transaction
import com.hisabnikash.app.core.model.TransactionType
import com.hisabnikash.app.core.money.MoneyParseResult
import com.hisabnikash.app.R
import com.hisabnikash.app.core.money.MoneyParser
import com.hisabnikash.app.data.repository.CategoryRepository
import com.hisabnikash.app.data.repository.PaymentMethodPreference
import com.hisabnikash.app.data.repository.TransactionRepository
import java.util.UUID
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

class QuickEntryViewModel(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val paymentMethodPreference: PaymentMethodPreference,
    private val nowEpochMillis: () -> Long = System::currentTimeMillis,
    private val newId: () -> String = { UUID.randomUUID().toString() },
) : ViewModel() {
    private val mutableState = MutableStateFlow(QuickEntryState(occurredAtEpochMillis = nowEpochMillis()))
    val state: StateFlow<QuickEntryState> = mutableState.asStateFlow()
    private var categoryJob: Job? = null
    private val submitMutex = Mutex()

    init {
        viewModelScope.launch {
            val method = paymentMethodPreference.get()
            mutableState.update { it.copy(paymentMethod = method) }
        }
        observeCategories(TransactionType.EXPENSE)
    }

    fun setType(type: TransactionType) {
        mutableState.update { it.copy(type = type, categoryId = null, customCategoryLabel = "", saveCustomCategory = false) }
        observeCategories(type)
    }

    fun setAmount(value: String) = mutableState.update { it.copy(amountInput = value, errors = it.errors - FormField.AMOUNT) }
    fun setDescription(value: String) = mutableState.update { it.copy(description = value, errors = it.errors - FormField.DESCRIPTION) }
    fun setCategory(id: String?) = mutableState.update {
        it.copy(categoryId = id, customCategoryLabel = "", saveCustomCategory = false, errors = it.errors - FormField.CATEGORY - FormField.CUSTOM_CATEGORY)
    }
    fun setCustomCategory(label: String, saveForLater: Boolean) = mutableState.update {
        it.copy(customCategoryLabel = label, saveCustomCategory = saveForLater, errors = it.errors - FormField.CUSTOM_CATEGORY)
    }
    fun setPaymentMethod(method: PaymentMethod) = mutableState.update { it.copy(paymentMethod = method) }
    fun setNote(value: String) = mutableState.update { it.copy(note = value) }
    fun setOccurredAt(epochMillis: Long) = mutableState.update { it.copy(occurredAtEpochMillis = epochMillis) }
    fun startNewEntry() = mutableState.update { it.copy(occurredAtEpochMillis = nowEpochMillis()) }

    suspend fun submit(intentionalDuplicate: Boolean = false): SubmitResult {
        if (!submitMutex.tryLock()) return SubmitResult.InProgress
        return try {
        val current = mutableState.value
        val amount = MoneyParser.parseToMinorUnits(current.amountInput)
        val errors = buildMap {
            if (amount !is MoneyParseResult.Success) put(FormField.AMOUNT, R.string.error_valid_amount)
            if (current.description.isBlank()) put(FormField.DESCRIPTION, R.string.error_description_required)
            if (current.categoryId == null) put(FormField.CATEGORY, R.string.error_choose_category)
            if (current.categoryId?.endsWith("_other") == true && current.customCategoryLabel.isBlank()) {
                put(FormField.CUSTOM_CATEGORY, R.string.error_category_name)
            }
        }
        if (errors.isNotEmpty()) {
            mutableState.update { it.copy(errors = errors) }
            return SubmitResult.ValidationFailed(errors)
        }

        val timestamp = nowEpochMillis()
        val customCategory = current.customCategoryLabel.trim().takeIf { current.saveCustomCategory && it.isNotEmpty() }?.let { label ->
            Category(
                id = "category-${newId()}",
                type = current.type.toCategoryType(),
                nameEn = label,
                nameBn = label,
                sortOrder = (current.categories.maxOfOrNull(Category::sortOrder) ?: 0) + 1,
                isSystem = false,
                createdAtEpochMillis = timestamp,
            )
        }
        val transaction = Transaction(
            id = newId(),
            type = current.type,
            amountMinor = (amount as MoneyParseResult.Success).minorUnits,
            description = current.description.trim(),
            categoryId = customCategory?.id ?: current.categoryId,
            oneOffCategoryLabel = current.customCategoryLabel.trim().takeIf { !current.saveCustomCategory && it.isNotEmpty() },
            paymentMethod = current.paymentMethod,
            note = current.note.trim().ifEmpty { null },
            occurredAtEpochMillis = current.occurredAtEpochMillis,
            createdAtEpochMillis = timestamp,
        )

        if (!intentionalDuplicate) {
            transactionRepository.findPossibleDuplicate(transaction)?.let { return SubmitResult.PossibleDuplicate(it) }
        }

        mutableState.update { it.copy(isSaving = true, saveError = null) }
        return try {
            if (customCategory != null) transactionRepository.addWithCustomCategory(transaction, customCategory)
            else transactionRepository.add(transaction)
            paymentMethodPreference.set(current.paymentMethod)
            mutableState.update {
                it.copy(
                    amountInput = "",
                    description = "",
                    categoryId = current.categories.firstOrNull()?.id,
                    customCategoryLabel = "",
                    saveCustomCategory = false,
                    note = "",
                    occurredAtEpochMillis = nowEpochMillis(),
                    isSaving = false,
                    errors = emptyMap(),
                )
            }
            SubmitResult.Saved(transaction.id)
        } catch (error: Exception) {
            mutableState.update { it.copy(isSaving = false, saveError = R.string.error_save_transaction) }
            SubmitResult.Failed(R.string.error_save_transaction)
        }
        } finally {
            submitMutex.unlock()
        }
    }

    private fun observeCategories(type: TransactionType) {
        categoryJob?.cancel()
        categoryJob = viewModelScope.launch {
            categoryRepository.observeActive(type.toCategoryType()).collect { categories ->
                mutableState.update { current ->
                    current.copy(
                        categories = categories,
                        categoryId = current.categoryId ?: categories.firstOrNull()?.id,
                    )
                }
            }
        }
    }
}

private fun TransactionType.toCategoryType(): CategoryType = when (this) {
    TransactionType.INCOME -> CategoryType.INCOME
    TransactionType.EXPENSE -> CategoryType.EXPENSE
}
