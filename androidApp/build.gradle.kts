import java.io.File as KeystoreFile
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.compose")
}

kotlin {
    compilerOptions.jvmTarget.set(JvmTarget.JVM_11)
}

android {
    namespace = "com.anchor.app"
    compileSdk = 36
    buildFeatures { buildConfig = true }

    val appVersionCode = when (val raw = findProperty("versionCode") as? String) {
        null, "" -> 1
        else -> raw.toIntOrNull() ?: error("versionCode 必须是整数，实际是 $raw")
    }
    val appVersionName = (findProperty("versionName") as? String)?.takeIf { it.isNotBlank() } ?: "0.1.0"

    defaultConfig {
        applicationId = "com.anchor.app"
        minSdk = 26
        targetSdk = 36
        versionCode = appVersionCode
        versionName = appVersionName
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("boolean", "UPDATE_ENABLED", "false")
    }

    signingConfigs {
        create("internal") {
            val path = System.getenv("ANCHOR_KEYSTORE")
            if (!path.isNullOrBlank()) {
                storeFile = file(path)
                storePassword = System.getenv("ANCHOR_KEYSTORE_PASSWORD").orEmpty()
                keyAlias = System.getenv("ANCHOR_KEY_ALIAS").orEmpty()
                keyPassword = System.getenv("ANCHOR_KEY_PASSWORD").orEmpty()
            }
        }
    }

    buildTypes {
        release {
        }
        create("internal") {
            initWith(getByName("release"))
            matchingFallbacks += listOf("release")
            buildConfigField("boolean", "UPDATE_ENABLED", "true")
            signingConfig = signingConfigs.getByName("internal")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    installation {
        timeOutInMs = 600_000
    }
}

val internalKeystorePath = providers.environmentVariable("ANCHOR_KEYSTORE")
tasks.configureEach {
    if (name == "assembleInternal") {
        doFirst {
            val path = internalKeystorePath.orNull
            require(!path.isNullOrBlank() && KeystoreFile(path).isFile) {
                "内部测试包需要签名钥。请设置 ANCHOR_KEYSTORE、ANCHOR_KEYSTORE_PASSWORD、ANCHOR_KEY_ALIAS、ANCHOR_KEY_PASSWORD。"
            }
        }
    }
}

dependencies {
    implementation(project(":shared"))
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.fragment:fragment:1.5.1")
    implementation("androidx.biometric:biometric:1.1.0")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test:runner:1.7.0")
}
