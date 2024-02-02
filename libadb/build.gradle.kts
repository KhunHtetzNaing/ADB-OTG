@file:Suppress("UnstableApiUsage")

plugins {
    alias(libs.plugins.library)
    alias(libs.plugins.kotlin)
}

android {
    namespace = "com.cgutman.adb"
    compileSdk = project.properties["compileSdk"].toString().toInt()
    ndkVersion = project.properties["ndkVersion"].toString()

    defaultConfig {
        minSdk = project.properties["minSdk"].toString().toInt()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")

        externalNativeBuild {
            cmake {
                arguments("-DANDROID_STL=none")
            }
        }
        ndk {
            abiFilters += setOf(
                "arm64-v8a", "armeabi-v7a", "x86_64", "x86"
            )
        }
    }

    buildFeatures {
        buildConfig = false
        androidResources = false
        prefab = true
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = project.properties["cmakeVersion"].toString()
        }
    }
}

dependencies {
    compileOnly(libs.boringssl)
    compileOnly(libs.cxx)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
