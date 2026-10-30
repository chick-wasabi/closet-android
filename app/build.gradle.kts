import groovy.json.JsonSlurper

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.kapt")
}
val signingFile = providers.environmentVariable("CLOSET_SIGNING_CONFIG").orNull?.let(::File)
val permanentSigning = signingFile?.takeIf { it.isFile }?.let { JsonSlurper().parse(it) as Map<*, *> }

android {
    namespace = "dev.closet"
    compileSdk = 36
    defaultConfig {
        applicationId = "dev.closet.personal"
        minSdk = 26
        targetSdk = 36
        versionCode = 4
        versionName = "1.3.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    signingConfigs {
        if (permanentSigning != null) create("permanent") {
            storeFile = file(permanentSigning["store_file"] as String)
            storePassword = permanentSigning["password"] as String
            keyAlias = permanentSigning["alias"] as String
            keyPassword = permanentSigning["password"] as String
        }
    }
    buildTypes {
        getByName("debug") { signingConfig = signingConfigs.findByName("permanent") }
        getByName("release") { signingConfig = signingConfigs.findByName("permanent") }
    }
    sourceSets.getByName("test").resources.srcDir("src/main/assets")
    buildFeatures { compose = true; buildConfig = true }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    testOptions {
        unitTests.isReturnDefaultValues = true
        unitTests.isIncludeAndroidResources = true
        unitTests.all {
            it.maxHeapSize = "1g"
            it.systemProperty("robolectric.dependency.repo.url", "https://repo.maven.apache.org/maven2")
            it.systemProperty("roborazzi.test.record", "true")
        }
    }
}
kapt { arguments { arg("room.schemaLocation", "$projectDir/schemas") } }

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.10.01"))
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")
    implementation("androidx.room:room-runtime:2.7.2")
    implementation("androidx.room:room-ktx:2.7.2")
    testImplementation("androidx.room:room-testing:2.7.2")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("com.google.mlkit:image-labeling:17.0.9")
    implementation("com.google.mlkit:text-recognition:16.0.1")
    kapt("androidx.room:room-compiler:2.7.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.10.2")
    implementation("androidx.exifinterface:exifinterface:1.4.1")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("com.google.android.gms:play-services-mlkit-subject-segmentation:16.0.0-beta1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2025.10.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test:runner:1.7.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    testImplementation(platform("androidx.compose:compose-bom:2025.10.01"))
    testImplementation("androidx.compose.ui:ui-test-junit4")
    testImplementation("org.robolectric:robolectric:4.16.1")
    testImplementation("io.github.takahirom.roborazzi:roborazzi:1.50.0")
    testImplementation("io.github.takahirom.roborazzi:roborazzi-compose:1.50.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20250517")
}

// Packaging must never fall back to a generated debug key or an unsigned APK.
val verifySigningIdentity by tasks.registering(Exec::class) {
    commandLine("python3", rootProject.file("scripts/signing.py"), "verify")
}
tasks.matching { it.name in setOf("packageDebug", "packageRelease", "signDebugBundle", "signReleaseBundle") }.configureEach {
    dependsOn(verifySigningIdentity)
}
