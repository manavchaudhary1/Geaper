import java.io.FileInputStream
import java.util.Properties
import org.gradle.internal.impldep.org.eclipse.jgit.util.RawCharUtil.trimTrailingWhitespace

val keystoreProperties = Properties()
val keystorePropertiesFile = rootProject.file("keystore.properties")

if (keystorePropertiesFile.exists()) {
  keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.ksp)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.spotless)
}

val youtubedlAndroid = "0.18.1"

android {
  namespace = "com.manav.geaper"
  compileSdk = 36

  defaultConfig {
    applicationId = "com.manav.geaper"
    minSdk = 26
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    create("release") {
      storeFile = file(keystoreProperties["storeFile"] as String)
      storePassword = keystoreProperties["storePassword"] as String
      keyAlias = keystoreProperties["keyAlias"] as String
      keyPassword = keystoreProperties["keyPassword"] as String
    }
  }

  buildTypes { release { signingConfig = signingConfigs.getByName("release") } }

  splits {
    abi {
      isEnable = true
      reset()

      include("arm64-v8a", "armeabi-v7a", "x86_64", "universal")

      isUniversalApk = true
    }
  }

  buildTypes {
    release {
      isMinifyEnabled = false

      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }

  buildFeatures { compose = true }

  packaging { jniLibs { useLegacyPackaging = true } }
}

dependencies {

  // Core
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)

  // Compose
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)

  // Navigation
  implementation(libs.androidx.navigation.compose)

  // ViewModel
  implementation(libs.androidx.lifecycle.viewmodel.compose)

  // Room
  implementation(libs.androidx.room.runtime)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.compose.animation.core)
  ksp(libs.androidx.room.compiler)

  // DataStore
  implementation("androidx.datastore:datastore-preferences:1.1.1")

  // Ktor HTTP
  implementation(libs.ktor.client.core)
  implementation(libs.ktor.client.cio)
  implementation(libs.ktor.client.content)
  implementation(libs.ktor.serialization)

  // Serialization
  implementation(libs.kotlinx.json)

  // Coroutines
  implementation(libs.kotlinx.coroutines)

  // yt-dlp + ffmpeg
  implementation("io.github.junkfood02.youtubedl-android:library:$youtubedlAndroid")
  implementation("io.github.junkfood02.youtubedl-android:ffmpeg:$youtubedlAndroid")

  // Testing
  testImplementation(libs.junit)

  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.espresso.core)

  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)

  debugImplementation(libs.androidx.compose.ui.tooling)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
}

spotless {
  kotlin {
    target("src/**/*.kt")
    targetExclude("build/**/*.kt")

    // ktfmt — Google's Kotlin formatter (used by AOSP / Jetpack)
    ktfmt("0.46").googleStyle()

    // Trim trailing whitespace and enforce Unix line endings
    trimTrailingWhitespace()
    endWithNewline()

    // Optional: enforce a copyright header on every file
    // licenseHeaderFile(rootProject.file("spotless/copyright.kt"))
  }

  kotlinGradle {
    target("*.gradle.kts")
    ktfmt("0.46").googleStyle()
    trimTrailingWhitespace()
    endWithNewline()
  }
}
