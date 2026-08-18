package com.anchor.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidPrivacySurfaceTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun installedPackageDoesNotRequestNetworkPermissions() {
        val info = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_PERMISSIONS,
        )
        val permissions = info.requestedPermissions?.toList().orEmpty()
        assertFalse(permissions.contains(Manifest.permission.INTERNET))
        assertFalse(permissions.contains(Manifest.permission.ACCESS_NETWORK_STATE))
        assertFalse(permissions.contains(Manifest.permission.ACCESS_WIFI_STATE))
    }
}
