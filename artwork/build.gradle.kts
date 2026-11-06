plugins {
    id("com.android.library")
}

android {
    namespace = "org.grimmoir.lunera.artwork"
    compileSdk = 37
    buildToolsVersion = "37.0.0"
    defaultConfig { minSdk = 26 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

kotlin { jvmToolchain(21) }

dependencies {
    implementation(libs.compose.runtime)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${libs.versions.coroutinesGuava.get()}")
    testImplementation(libs.junit)
}
