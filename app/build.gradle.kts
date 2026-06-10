plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.alexp.anydownload"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.alexp.anydownload"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "1.0.2"

        buildConfigField("String", "GITHUB_OWNER", "\"apotapovax\"")
        buildConfigField("String", "GITHUB_REPO", "\"any-download\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        splits {
            abi {
                isEnable = true
                reset()
                include("arm64-v8a")
                isUniversalApk = false
            }
        }
    }

    signingConfigs {
        create("release") {
            val keystorePath = System.getenv("ANDROID_KEYSTORE_PATH")
                ?: project.findProperty("RELEASE_STORE_FILE") as String?
            val keystoreFile = keystorePath?.let(::file)?.takeIf { it.exists() }

            if (keystoreFile != null) {
                storeFile = keystoreFile
                storePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
                    ?: project.findProperty("RELEASE_STORE_PASSWORD") as String?
                keyAlias = System.getenv("ANDROID_KEY_ALIAS")
                    ?: project.findProperty("RELEASE_KEY_ALIAS") as String?
                keyPassword = System.getenv("ANDROID_KEY_PASSWORD")
                    ?: project.findProperty("RELEASE_KEY_PASSWORD") as String?
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.getByName("release").takeIf {
                it.storeFile?.exists() == true
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    val youtubedlVersion = "0.18.1"

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    implementation("io.github.junkfood02.youtubedl-android:library:$youtubedlVersion")
    implementation("io.github.junkfood02.youtubedl-android:ffmpeg:$youtubedlVersion")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
