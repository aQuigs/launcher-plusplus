plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.aquigs.launcherplusplus"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.aquigs.launcherplusplus"
        minSdk = 30
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

// Scripts headed "# Shared script:" are verbatim copies of same-named files in a separate tooling checkout. When that
// checkout's sync-common is on PATH, every build refreshes the copies so they cannot drift; otherwise nothing runs.
val syncCommon = System.getenv("PATH").orEmpty().split(File.pathSeparator)
    .map { File(it, "sync-common") }
    .firstOrNull { it.canExecute() }
val syncSharedScripts by tasks.registering(Exec::class) {
    enabled = syncCommon != null
    workingDir = rootDir
    commandLine(syncCommon?.path ?: "sync-common", "scripts")
}
tasks.named("preBuild") { dependsOn(syncSharedScripts) }

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    testImplementation(libs.junit)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
}
