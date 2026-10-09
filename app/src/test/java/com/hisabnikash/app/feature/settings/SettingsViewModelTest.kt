package com.hisabnikash.app.feature.settings

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import com.hisabnikash.app.R
import com.hisabnikash.app.backup.*
import com.hisabnikash.app.backup.crypto.BackupCrypto
import com.hisabnikash.app.backup.crypto.BackupPasswordCache
import com.hisabnikash.app.backup.drive.*
import com.hisabnikash.app.backup.local.*
import com.hisabnikash.app.data.db.HisabDatabase
import com.hisabnikash.app.data.preferences.UserPreferences
import com.hisabnikash.app.data.repository.ClearableRepository
import com.hisabnikash.app.feature.transactions.FakeCategoryRepository
import com.hisabnikash.app.feature.transactions.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SettingsViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()
    private lateinit var context: Context
    private lateinit var database: HisabDatabase
    private val models = ViewModelStore()
    @Before fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = HisabDatabase.createInMemory(context)
    }
    @After fun tearDown() { models.clear(); database.close() }

    @Test fun `passwords are wiped if ViewModel was cleared before launch starts`() = runTest {
        val fixture = fixture()
        models.clear()
        val passwords = List(5) { "fixture-password".toCharArray() }
        fixture.vm.backUpNow(passwords[0])
        fixture.vm.restore(Uri.parse("content://fixture"), passwords[1])
        fixture.vm.driveBackUpNow(passwords[2])
        fixture.vm.driveRestoreLatest(passwords[3])
        fixture.vm.undoLastRestore(passwords[4])
        runCurrent()
        assertTrue(passwords.all { password -> password.all { it == '\u0000' } })
    }

    @Test fun `in flight authorization cancellation wipes supplied backup password`() = runTest {
        val entered = CompletableDeferred<Unit>()
        val auth = FixtureAuthorization().apply { token = { entered.complete(Unit); awaitCancellation() } }
        val fixture = fixture(auth)
        val password = "fixture-password".toCharArray()
        fixture.vm.driveBackUpNow(password)
        runCurrent()
        entered.await()
        models.clear()
        runCurrent()
        assertTrue(password.all { it == '\u0000' })
    }

    @Test fun `failed local deletion gives localized failure and never signals deletion`() = runTest {
        var deleted = false
        val fixture = fixture(deletion = DataDeletionUseCase(listOf(object : ClearableRepository {
            override suspend fun clearAll() { error("fixture failure") }
        })) {})
        fixture.vm.deleteAllLocalData { deleted = true }
        runCurrent()
        assertEquals(R.string.operation_failed, fixture.vm.backupMessage.value)
        assertFalse(deleted)
    }

    @Test fun `cancelled Drive consent clears resolution so it can be launched again`() = runTest {
        val pending = PendingIntent.getActivity(context, 42, Intent(context, Activity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val auth = FixtureAuthorization().apply { authorization = DriveAuthorizationResult.ResolutionRequired(pending) }
        val fixture = fixture(auth)
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        fixture.vm.authorizeDrive(activity)
        runCurrent()
        assertEquals(pending, fixture.vm.driveResolution.value)
        fixture.vm.completeDriveAuthorization(null)
        assertNull(fixture.vm.driveResolution.value)
        fixture.vm.authorizeDrive(activity)
        runCurrent()
        assertEquals(pending, fixture.vm.driveResolution.value)
    }

    @Test fun `failed consent completion clears pending resolution`() = runTest {
        val pending = PendingIntent.getActivity(context, 43, Intent(context, Activity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val auth = FixtureAuthorization().apply { authorization = DriveAuthorizationResult.ResolutionRequired(pending) }
        val fixture = fixture(auth)
        fixture.vm.authorizeDrive(Robolectric.buildActivity(Activity::class.java).setup().get())
        runCurrent()
        fixture.vm.completeDriveAuthorization(Intent())
        assertNull(fixture.vm.driveResolution.value)
        assertEquals(R.string.drive_auth_failed, fixture.vm.backupMessage.value)
    }

    private suspend fun fixture(auth: FixtureAuthorization = FixtureAuthorization(), deletion: DataDeletionUseCase = DataDeletionUseCase(emptyList()) {}): Fixture {
        val preferences = UserPreferences.create(context)
        preferences.clearPrivateState()
        val gate = BackupOperationGate()
        val exporter = SnapshotExporter(database)
        val importer = SnapshotImporter(database, BackupCrypto(10_000))
        val cache = FixtureSecrets()
        val local = LocalBackupStore(object : LocalDocumentAccess {
            override fun persistTreeAccess(uri: Uri) = true
            override fun writeAtomically(treeUri: Uri, fileName: String, bytes: ByteArray) = Unit
            override fun read(uri: Uri) = byteArrayOf()
        }, preferences, exporter, importer, cache, operationGate = gate)
        val drive = DriveBackupStore(object : DriveRemoteStore {
            override suspend fun uploadLatest(accessToken: String, encryptedEnvelope: ByteArray) = Unit
            override suspend fun downloadLatest(accessToken: String): ByteArray? = null
        }, exporter, importer, operationGate = gate, isEnabled = { preferences.state.first().driveBackupEnabled }, setEnabled = preferences::setDriveBackupEnabled, passwordCache = cache)
        val vm = SettingsViewModel(preferences, deletion, local, auth, drive, cache, NoOpRestoreRecoveryStore, FakeCategoryRepository())
        models.put("settings", vm)
        return Fixture(vm, drive, cache)
    }

    private data class Fixture(val vm: SettingsViewModel, val drive: DriveBackupStore, val cache: FixtureSecrets)
    private class FixtureAuthorization : DriveAuthorizationGateway {
        var authorization: DriveAuthorizationResult = DriveAuthorizationResult.Authorized("fixture-token")
        var token: suspend () -> String? = { "fixture-token" }
        override suspend fun authorizeDriveAppData(activity: Activity) = authorization
        override suspend fun currentAccessToken() = token()
        override fun completeResolution(data: Intent) = DriveAuthorizationResult.Failed(IllegalStateException("fixture failure"))
        override suspend fun revoke() = Unit
    }
    private class FixtureSecrets : BackupPasswordCache {
        private var password: CharArray? = null
        override fun save(password: CharArray) { this.password = password.copyOf() }
        override fun load() = password?.copyOf()
        override fun clear() { password?.fill('\u0000'); password = null }
    }
}
