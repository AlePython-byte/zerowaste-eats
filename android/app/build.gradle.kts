import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.secrets.gradle.plugin)
}

// Endpoints come from local.properties, which is never committed (see local.properties.example)
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use(::load)
}

fun localProperty(name: String, default: String = ""): String =
    localProperties.getProperty(name)?.let(LocalProperties::clean) ?: default

/** A URL from local.properties. A malformed one stops the build instead of crashing the app. */
fun endpointProperty(name: String, default: String = "", trailingSlash: Boolean = false): String =
    LocalProperties.requireEndpoint(name, localProperty(name, default), trailingSlash)

/** A Kotlin string literal for a BuildConfig field. */
fun stringField(value: String): String =
    "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

android {
    namespace = "com.zerowasteeats.app"

    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.zerowasteeats.app"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // The publishable key is public by design (Supabase pairs it with row level
        // security), but it still stays out of the repository
        buildConfigField("String", "SUPABASE_URL", stringField(endpointProperty("SUPABASE_URL")))
        buildConfigField(
            "String",
            "SUPABASE_PUBLISHABLE_KEY",
            stringField(localProperty("SUPABASE_PUBLISHABLE_KEY"))
        )
    }

    buildTypes {
        debug {
            // The emulator reaches the computer running the backend at 10.0.2.2
            buildConfigField(
                "String",
                "API_BASE_URL",
                stringField(
                    endpointProperty(
                        "API_BASE_URL_DEBUG",
                        default = "http://10.0.2.2:3000/api/v1/",
                        trailingSlash = true
                    )
                )
            )
        }
        release {
            // Empty until the backend is deployed; the app then offers demo data only
            buildConfigField(
                "String",
                "API_BASE_URL",
                stringField(endpointProperty("API_BASE_URL_RELEASE", trailingSlash = true))
            )
            optimization {
                enable = false
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    androidResources {
        generateLocaleConfig = true
    }
}

secrets {
    // These become BuildConfig fields per build type above; the plugin only handles the maps key
    ignoreList.add("API_BASE_URL_.*")
    ignoreList.add("SUPABASE_.*")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)

    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.compose)

    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.maps.compose)
    implementation(libs.play.services.location)
    implementation(libs.androidx.exifinterface)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.okhttp.mockwebserver)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    debugImplementation(libs.androidx.compose.ui.test.manifest)

    implementation(libs.material)
}