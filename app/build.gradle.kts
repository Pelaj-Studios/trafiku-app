import java.net.URI

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

val releaseServerBaseUrl = providers.gradleProperty("trafiku.server.baseUrl")
    .orElse("https://example.invalid/")
    .map { value ->
        val uri = runCatching { URI(value) }.getOrNull()
        if (
            uri == null ||
            uri.scheme != "https" ||
            uri.host.isNullOrBlank() ||
            uri.rawQuery != null ||
            uri.rawFragment != null ||
            !value.endsWith("/")
        ) {
            throw GradleException(
                "trafiku.server.baseUrl must be an absolute HTTPS URL with a final slash and no query or fragment.",
            )
        }
        value
    }
val appVersionCode = providers.gradleProperty("trafiku.versionCode")
    .orElse("1")
    .map { value ->
        value.toIntOrNull()?.takeIf { versionCode -> versionCode > 0 }
            ?: throw GradleException("trafiku.versionCode must be a positive integer.")
    }
val appVersionName = providers.gradleProperty("trafiku.versionName")
    .orElse("1.0.0")
val releaseSigningValues = listOf(
    providers.environmentVariable("TRAFIKU_RELEASE_KEYSTORE_PATH"),
    providers.environmentVariable("TRAFIKU_RELEASE_KEYSTORE_PASSWORD"),
    providers.environmentVariable("TRAFIKU_RELEASE_KEY_ALIAS"),
    providers.environmentVariable("TRAFIKU_RELEASE_KEY_PASSWORD"),
)
val releaseSigningConfigured = releaseSigningValues.all { value -> value.isPresent }
val releaseSigningPartiallyConfigured = releaseSigningValues.any { value -> value.isPresent } &&
    !releaseSigningConfigured

if (releaseSigningPartiallyConfigured) {
    throw GradleException("Set all TRAFIKU_RELEASE_* environment variables or set none of them.")
}

android {
    namespace = "com.pelajstudios.trafiku"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.pelajstudios.trafiku"
        minSdk = 36
        targetSdk = 36
        versionCode = appVersionCode.get()
        versionName = appVersionName.get()
    }

    signingConfigs {
        if (releaseSigningConfigured) {
            create("release") {
                storeFile = file(releaseSigningValues[0].get())
                storePassword = releaseSigningValues[1].get()
                keyAlias = releaseSigningValues[2].get()
                keyPassword = releaseSigningValues[3].get()
            }
        }
    }

    buildTypes {
        debug {
            buildConfigField(
                "String",
                "TRAFIKU_SERVER_BASE_URL",
                "\"http://10.0.2.2:8080/\"",
            )
        }
        release {
            buildConfigField(
                "String",
                "TRAFIKU_SERVER_BASE_URL",
                "\"${releaseServerBaseUrl.get()}\"",
            )
            if (releaseSigningConfigured) {
                signingConfig = signingConfigs.getByName("release")
            }
            optimization {
                enable = true
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        buildConfig = true
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui.compose)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.tv.foundation)
    implementation(libs.androidx.tv.material)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.usb.serial)
    ksp(libs.androidx.room.compiler)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
