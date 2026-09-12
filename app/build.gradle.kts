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

// Scripts marked "# shared-source:" are copies of common-configs files. Every build refreshes them from the
// local checkout so a copy cannot drift; clones without that checkout, or a checkout without the tool, skip it.
val commonConfigs = file(System.getenv("COMMON_CONFIGS") ?: "${System.getProperty("user.home")}/repos/common-configs")
val syncTool = commonConfigs.resolve("bin/sync-common")
val syncSharedScripts by tasks.registering(Exec::class) {
    // A local copy keeps the predicate free of script references, which the configuration cache cannot serialize.
    val tool = syncTool
    onlyIf { tool.canExecute() }
    workingDir = rootDir
    commandLine(tool.path, "scripts")
}
tasks.named("preBuild") { dependsOn(syncSharedScripts) }

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    testImplementation(libs.junit)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
}
