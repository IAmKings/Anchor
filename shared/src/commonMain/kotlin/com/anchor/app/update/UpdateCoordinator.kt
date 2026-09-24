package com.anchor.app.update

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

sealed interface UpdatePhase {
    data object Idle : UpdatePhase
    data object Checking : UpdatePhase
    data object UpToDate : UpdatePhase
    data class UpdateAvailable(
        val versionName: String,
        val versionCode: Int,
        val apkUrl: String,
    ) : UpdatePhase
    data object Failed : UpdatePhase
}

class UpdateCoordinator(
    private val localVersionCode: Int,
    private val fetchFeed: suspend () -> String,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    private val lock = Any()
    private val phases = MutableStateFlow<UpdatePhase>(UpdatePhase.Idle)
    val phase: StateFlow<UpdatePhase> = phases
    private var job: Job? = null
    private var generation = 0
    private var dismissedCode: Int? = null

    fun check() {
        val ticket: Int
        synchronized(lock) {
            ticket = ++generation
            job?.cancel()
            job = scope.launch {
                phases.value = UpdatePhase.Checking
                val next = try {
                    interpretFeed(localVersionCode, parseReleaseFeed(fetchFeed()))
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    UpdatePhase.Failed
                }
                if (!isActive) return@launch
                synchronized(lock) {
                    if (ticket == generation) phases.value = next
                }
            }
        }
    }

    fun dismiss() {
        val available = phases.value as? UpdatePhase.UpdateAvailable ?: return
        dismissedCode = available.versionCode
    }

    fun shouldPrompt(): Boolean {
        val available = phases.value as? UpdatePhase.UpdateAvailable ?: return false
        return dismissedCode != available.versionCode
    }

    internal suspend fun joinLatest() {
        val current = synchronized(lock) { job }
        current?.join()
    }
}
