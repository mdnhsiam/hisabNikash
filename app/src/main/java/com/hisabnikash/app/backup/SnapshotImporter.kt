package com.hisabnikash.app.backup

import androidx.room.withTransaction
import com.hisabnikash.app.backup.crypto.BackupCrypto
import com.hisabnikash.app.backup.model.BackupSnapshotV1
import com.hisabnikash.app.backup.model.BackupWireCodec
import com.hisabnikash.app.data.db.HisabDatabase
import kotlinx.serialization.SerializationException

sealed interface RestoreResult {
    data object Success : RestoreResult
    data class Failed(val cause: Throwable) : RestoreResult
}

class SnapshotImporter(
    private val database: HisabDatabase,
    private val crypto: BackupCrypto = BackupCrypto(),
    private val beforeCommit: suspend () -> Unit = {},
) {
    fun encode(snapshot: BackupSnapshotV1, password: CharArray): ByteArray {
        val clear = BackupWireCodec.encodeSnapshot(snapshot)
        return try {
            BackupWireCodec.encodeEnvelope(crypto.encrypt(clear, password))
        } finally {
            clear.fill(0)
        }
    }

    fun validate(bytes: ByteArray, password: CharArray): ValidatedRestore {
        val envelope = try {
            BackupWireCodec.decodeEnvelope(bytes)
        } catch (error: SerializationException) {
            throw InvalidBackupException("Invalid backup envelope", error)
        }
        if (envelope.formatVersion != BackupCrypto.FORMAT_VERSION) {
            throw UnsupportedBackupVersionException(envelope.formatVersion)
        }
        val clear = crypto.decrypt(envelope, password)
        return try {
            val snapshot = try {
                BackupWireCodec.decodeSnapshot(clear)
            } catch (error: SerializationException) {
                throw InvalidBackupException("Invalid backup snapshot", error)
            }
            RestoreValidator.validate(snapshot)
        } finally {
            clear.fill(0)
        }
    }

    suspend fun apply(validated: ValidatedRestore): RestoreResult = try {
        database.withTransaction {
            val snapshot = validated.snapshot
            database.snapshotDao().replaceAll(
                categories = snapshot.categories,
                transactions = snapshot.transactions,
                budgets = snapshot.budgets,
                savingsGoals = snapshot.savingsGoals,
                goalAllocations = snapshot.goalAllocations,
                recurringItems = snapshot.recurringItems,
            )
            beforeCommit()
        }
        RestoreResult.Success
    } catch (error: Throwable) {
        RestoreResult.Failed(error)
    }
}
