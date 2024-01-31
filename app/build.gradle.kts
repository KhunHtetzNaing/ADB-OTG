@file:Suppress("UnstableApiUsage")

plugins {
    alias(libs.plugins.application)
    alias(libs.plugins.kotlin)
    alias(libs.plugins.materialthemebuilder)
}

android {
    namespace = "com.htetznaing.adbotg"
    compileSdk = project.properties["compileSdk"].toString().toInt()

    defaultConfig {
        applicationId = "com.htetznaing.adbotg"
        minSdk = project.properties["minSdk"].toString().toInt()
        targetSdk = 34
        versionCode = 2
        versionName = "1.0.1"
        resourceConfigurations += "en"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
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

    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    lint {
        checkReleaseBuilds = false
        abortOnError = true
    }

    materialThemeBuilder {
        themes {
            create("AdbOtg") {
                primaryColor = "#3F51B5"

                lightThemeFormat = "Theme.Material3.Light.%s"
                lightThemeParent = "Theme.Material3.Light"
                darkThemeFormat = "Theme.Material3.Dark.%s"
                darkThemeParent = "Theme.Material3.Dark"
            }
        }
        generatePaletteAttributes = true
        generateTextColors = true
    }
}

dependencies {
    implementation(project(":libadb"))

    implementation(libs.core.ktx)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)

    implementation(libs.autoimageslider)
    implementation(libs.glide)
    annotationProcessor(libs.glide.compiler)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
