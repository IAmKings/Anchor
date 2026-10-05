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

    val appVersionCode = when (
        val raw = (findProperty("versionCode") as? String)?.takeIf { it.isNotBlank() }
            ?: findProperty("anchorVersionCode") as? String
    ) {
        null, "" -> error("缺少 versionCode")
        else -> raw.toIntOrNull() ?: error("versionCode 必须是整数，实际是 $raw")
    }
    val appVersionName = (findProperty("versionName") as? String)?.takeIf { it.isNotBlank() }
        ?: (findProperty("anchorVersionName") as? String)?.takeIf { it.isNotBlank() }
        ?: error("缺少 versionName")

    defaultConfig {
        applicationId = "com.anchor.app"
        minSdk = 26
        targetSdk = 36
        versionCode = appVersionCode
        versionName = appVersionName
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("boolean", "UPDATE_ENABLED", "false")
    }

    flavorDimensions += "channel"
    productFlavors {
        // 默认渠道：GitHub Release 与国内渠道。USE_EXACT_ALARM 随主清单生效（API 33+ 自动授予）。
        create("standard") {
            dimension = "channel"
        }
        // Google Play 渠道：src/play/AndroidManifest.xml 移除 USE_EXACT_ALARM（Play 政策限制）。
        create("play") {
            dimension = "channel"
        }
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

// 有渠道后 AGP 不再生成 installDebug（assemble* 聚合任务仍由 AGP 生成，会带上两个渠道）。
// QA 脚本与文档的旧命令继续指向默认渠道 standard。
tasks.register("installDebug") { dependsOn("installStandardDebug") }

dependencies {
    implementation(project(":shared"))
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.fragment:fragment:1.5.1")
    implementation("androidx.biometric:biometric:1.1.0")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test:runner:1.7.0")
}
