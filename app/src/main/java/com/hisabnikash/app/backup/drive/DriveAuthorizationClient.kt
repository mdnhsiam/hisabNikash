package com.hisabnikash.app.backup.drive

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.RevokeAccessRequest
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

sealed interface DriveAuthorizationResult {
    data class Authorized(val accessToken: String) : DriveAuthorizationResult
    data class ResolutionRequired(val pendingIntent: PendingIntent) : DriveAuthorizationResult
    data class Failed(val cause: Throwable) : DriveAuthorizationResult
}

interface DriveAuthorizationGateway {
    suspend fun authorizeDriveAppData(activity: Activity): DriveAuthorizationResult
    suspend fun currentAccessToken(): String?
    fun completeResolution(data: Intent): DriveAuthorizationResult
    suspend fun revoke()
}

class DriveAuthorizationClient(private val context: Context) : DriveAuthorizationGateway {
    private val request: AuthorizationRequest
        get() = AuthorizationRequest.builder()
            .setRequestedScopes(listOf(Scope(DriveRestClient.DRIVE_APPDATA_SCOPE)))
            .build()

    override suspend fun authorizeDriveAppData(activity: Activity): DriveAuthorizationResult =
        authorizationResult(Identity.getAuthorizationClient(activity).authorize(request))

    override suspend fun currentAccessToken(): String? = when (
        val result = authorizationResult(Identity.getAuthorizationClient(context).authorize(request))
    ) {
        is DriveAuthorizationResult.Authorized -> result.accessToken
        else -> null
    }

    override fun completeResolution(data: Intent): DriveAuthorizationResult = runCatching {
        val result = Identity.getAuthorizationClient(context).getAuthorizationResultFromIntent(data)
        result.accessToken?.let(DriveAuthorizationResult::Authorized)
            ?: throw DriveReauthorizationRequired()
    }.getOrElse(DriveAuthorizationResult::Failed)

    override suspend fun revoke() {
        Identity.getAuthorizationClient(context).revokeAccess(RevokeAccessRequest.builder().build()).await()
    }

    private suspend fun authorizationResult(task: Task<com.google.android.gms.auth.api.identity.AuthorizationResult>): DriveAuthorizationResult =
        try {
            val result = task.await()
            when {
                result.hasResolution() -> DriveAuthorizationResult.ResolutionRequired(requireNotNull(result.pendingIntent))
                result.accessToken != null -> DriveAuthorizationResult.Authorized(result.accessToken!!)
                else -> DriveAuthorizationResult.Failed(DriveReauthorizationRequired())
            }
        } catch (error: Throwable) {
            DriveAuthorizationResult.Failed(error)
        }
}

private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { result -> continuation.resume(result) }
    addOnFailureListener { error -> continuation.resumeWithException(error) }
}
