import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}
// Android native SDK initialization requires the project JavaScript URL. A local
// property can override this value for a different SaleSmartly project/environment.
val configuredSalesmartlyScriptUrl = localProperties
    .getProperty("salesmartly.scriptUrl", "")
    .trim()
val salesmartlyScriptUrl = configuredSalesmartlyScriptUrl.ifBlank {
    "https://plugin-code.salesmartly.com/js/project_830957_862319_1789665876.js"
}
val configuredSalesmartlyWebUrl = localProperties
    .getProperty("salesmartly.webUrl", "")
    .trim()

android {
    namespace = "com.thrustwealth.app"
    // SaleSmartly chatwidget-sdk-v1.0.3 requires compileSdk 36+
    compileSdk = 36

    defaultConfig {
        applicationId = "com.thrustwealth.tools"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField(
            "String",
            "SALESMARTLY_SCRIPT_URL",
            "\"${salesmartlyScriptUrl.replace("\"", "\\\"")}\"",
        )
        buildConfigField(
            "String",
            "SALESMARTLY_WEB_URL",
            "\"${configuredSalesmartlyWebUrl.replace("\"", "\\\"")}\"",
        )
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    applicationVariants.all {
        val variant = this
        outputs.configureEach {
            val output = this
            if (output is com.android.build.gradle.internal.api.BaseVariantOutputImpl) {
                val formattedDate = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
                val appName = "ThrustWealth"
                output.outputFileName = "${appName}_v${variant.versionName}_${formattedDate}.apk"
            }
        }
    }
}

tasks.matching { it.name.startsWith("bundle") && (it.name.endsWith("Release") || it.name.endsWith("Debug")) }.configureEach {
    doLast {
        val isRelease = name.endsWith("Release")
        val buildTypeName = if (isRelease) "release" else "debug"
        val bundleDir = layout.buildDirectory.dir("outputs/bundle/$buildTypeName").get().asFile
        val originAab = File(bundleDir, "app-$buildTypeName.aab")
        if (originAab.exists()) {
            val formattedDate = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
            val appName = "ThrustWealth"
            val targetName = "${appName}_v${android.defaultConfig.versionName}_${formattedDate}.aab"
            originAab.copyTo(File(bundleDir, targetName), overwrite = true)
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    implementation(libs.androidx.navigation.compose)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    // SaleSmartly Chat SDK (local AAR + transitive deps)
    implementation(files("libs/salesmartly-chatwidget-sdk-v1.0.3.aar"))
    implementation(libs.okhttp)
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.socket.io.client) {
        exclude(group = "org.json", module = "json")
    }

    implementation(libs.appsflyer)
    implementation(libs.play.services.ads.identifier)
    implementation(libs.coil.compose)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
