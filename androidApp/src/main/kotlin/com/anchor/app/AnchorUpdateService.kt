package com.anchor.app

import com.anchor.app.update.UpdateCoordinator
import com.anchor.app.update.UpdatePhase
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

object AnchorUpdateService {
    private const val feedUrl = "https://api.github.com/repos/IAmKings/Anchor/releases?per_page=30"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val activeConnection = AtomicReference<HttpURLConnection?>(null)
    private var coordinator: UpdateCoordinator? = null
    private var onPhase: (UpdatePhase) -> Unit = {}
    private var openUrl: (String) -> Unit = {}

    @Synchronized
    fun ensureStarted(
        localVersionCode: Int,
        onPhase: (UpdatePhase) -> Unit,
        openUrl: (String) -> Unit,
    ) {
        this.onPhase = onPhase
        this.openUrl = openUrl
        val existing = coordinator
        if (existing != null) {
            onPhase(existing.phase.value)
            return
        }
        val created = UpdateCoordinator(localVersionCode, ::fetchFeed, scope)
        coordinator = created
        scope.launch {
            created.phase.collect { phase ->
                val listener = synchronized(this@AnchorUpdateService) { this@AnchorUpdateService.onPhase }
                withContext(Dispatchers.Main) { listener(phase) }
            }
        }
        created.check()
    }

    fun check() {
        coordinator?.check()
    }

    fun dismiss() {
        coordinator?.dismiss()
    }

    fun shouldPrompt(): Boolean = coordinator?.shouldPrompt() == true

    fun openDownload() {
        val offer = coordinator?.phase?.value as? UpdatePhase.UpdateAvailable ?: return
        openUrl(offer.apkUrl)
    }

    private suspend fun fetchFeed(): String = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine { cont ->
            val connection = (URL(feedUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 8_000
                readTimeout = 8_000
                instanceFollowRedirects = true
                requestMethod = "GET"
                setRequestProperty("User-Agent", "Anchor-Internal-Update")
                setRequestProperty("Accept", "application/vnd.github+json")
            }
            activeConnection.set(connection)
            cont.invokeOnCancellation { connection.disconnect() }
            try {
                val code = connection.responseCode
                val text = (if (code in 200..299) connection.inputStream else connection.errorStream)
                    ?.bufferedReader()
                    ?.use { it.readText() }
                    .orEmpty()
                if (!cont.isActive) return@suspendCancellableCoroutine
                if (code in 200..299) cont.resume(text) else cont.resumeWithException(IllegalStateException("http $code"))
            } catch (error: Exception) {
                if (cont.isActive) cont.resumeWithException(error)
            } finally {
                activeConnection.compareAndSet(connection, null)
                connection.disconnect()
            }
        }
    }
}
