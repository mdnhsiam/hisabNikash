package com.hisabnikash.app.data.db

import androidx.room.TypeConverter
import com.hisabnikash.app.core.model.CategoryType
import com.hisabnikash.app.core.model.PaymentMethod
import com.hisabnikash.app.core.model.RecurrenceFrequency
import com.hisabnikash.app.core.model.ReminderStyle
import com.hisabnikash.app.core.model.TransactionType

class DbConverters {
    @TypeConverter fun transactionType(value: TransactionType): String = value.name
    @TypeConverter fun transactionType(value: String): TransactionType = TransactionType.valueOf(value)
    @TypeConverter fun categoryType(value: CategoryType): String = value.name
    @TypeConverter fun categoryType(value: String): CategoryType = CategoryType.valueOf(value)
    @TypeConverter fun paymentMethod(value: PaymentMethod): String = value.name
    @TypeConverter fun paymentMethod(value: String): PaymentMethod = PaymentMethod.valueOf(value)
    @TypeConverter fun frequency(value: RecurrenceFrequency): String = value.name
    @TypeConverter fun frequency(value: String): RecurrenceFrequency = RecurrenceFrequency.valueOf(value)
    @TypeConverter fun reminderStyle(value: ReminderStyle): String = value.name
    @TypeConverter fun reminderStyle(value: String): ReminderStyle = ReminderStyle.valueOf(value)
}
