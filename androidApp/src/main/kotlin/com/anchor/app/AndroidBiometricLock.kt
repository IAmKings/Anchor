package com.anchor.app

import android.content.Context
import android.view.Window
import android.view.WindowManager
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

fun applyPrivacyShield(window: Window, enabled: Boolean) {
    if (enabled) {
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
    } else {
        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }
}

class AndroidBiometricLock(
    private val activity: FragmentActivity,
    preferencesName: String = PREFERENCES,
) {
    private val preferences = activity.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)

    var enabled: Boolean
        get() = preferences.getBoolean(ENABLED, false)
        set(value) = preferences.edit().putBoolean(ENABLED, value).apply()

    fun isAvailable(): Boolean =
        BiometricManager.from(activity).canAuthenticate(AUTHENTICATORS) ==
            BiometricManager.BIOMETRIC_SUCCESS

    fun authenticate(
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    ) {
        if (!isAvailable()) {
            onError("请先在系统设置中录入生物识别或设置设备锁。")
            return
        }
        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    onError(errString.toString())
                }
            },
        )
        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("解锁锚点")
                .setSubtitle("验证后才能查看本地记录")
                .setAllowedAuthenticators(AUTHENTICATORS)
                .build(),
        )
    }

    companion object {
        internal const val PREFERENCES = "app-lock"
        private const val ENABLED = "enabled"
        private const val AUTHENTICATORS =
            BiometricManager.Authenticators.BIOMETRIC_WEAK or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
    }
}
