
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.zeroproject.alice"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.zeroproject.alice"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("alice") {
            storeFile = file("alice-release.p12")
            storeType = "PKCS12"
            storePassword = System.getenv("ALICE_STORE_PASSWORD")
            keyAlias = System.getenv("ALICE_KEY_ALIAS")
            keyPassword = System.getenv("ALICE_KEY_PASSWORD")
        }
    }

    buildTypes {
        debug {
            isDebuggable = true
            signingConfig = signingConfigs.getByName("alice")
        }

        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("alice")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(
            org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
        )
    }
}

dependencies {
    implementation("com.google.ai.edge.litertlm:litertlm-android:0.18.0")
}
