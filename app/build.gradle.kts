import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
        alias(libs.plugins.android.application)
        alias(libs.plugins.kotlin.android)
        id("com.google.gms.google-services")
    }

    android {
        namespace = "com.mesawa.cuidarproximo"
        compileSdk = 37

        defaultConfig {
            applicationId = "com.mesawa.cuidarproximo"
            minSdk = 25
            targetSdk = 35
            versionCode = 1
            versionName = "1.0"

            testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }

        buildTypes {
            release {
                isMinifyEnabled = false
                proguardFiles(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    "proguard-rules.pro"
                )
            }
        }
        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_21
            targetCompatibility = JavaVersion.VERSION_21
        }

                kotlin {
                    compilerOptions {
                        jvmTarget.set(JvmTarget.JVM_21)
                    }
                }
        buildFeatures {
            viewBinding = true
        }
    }

    configurations.all {
        resolutionStrategy.force("com.google.firebase:firebase-functions:21.2.1")
        resolutionStrategy.force("com.squareup.okhttp3:okhttp:3.14.9")
        resolutionStrategy.force("com.squareup.okio:okio:1.17.2")
    }

    dependencies {

        implementation(platform(libs.firebase.bom))

        // 🔥 Firebase (VERSÃO LIMPA)
        implementation(libs.firebase.firestore)
        implementation(libs.firebase.core)
        implementation(libs.firebase.firestore.ktx.v2514)
        implementation(libs.firebase.auth.ktx.v2321)
       // implementation(platform(libs.firebase.bom.v34141))

       // implementation(libs.firebase.functions.ktx)
        implementation(libs.google.firebase.analytics)
        implementation(libs.jbcrypt)
        implementation(libs.firebase.functions)
        // Outros
        implementation(libs.androidx.core.ktx)
        implementation(libs.androidx.appcompat)
        //noinspection LoginCredentials
        implementation(libs.play.services.auth)
        implementation(libs.material)
        implementation(libs.androidx.constraintlayout)
        implementation(libs.androidx.lifecycle.livedata.ktx)
        implementation(libs.androidx.lifecycle.viewmodel.ktx)
        implementation(libs.androidx.navigation.fragment.ktx)
        implementation(libs.androidx.navigation.ui.ktx)
        implementation(libs.firebase.database)
        implementation(libs.play.services.location)

        testImplementation(libs.junit)
        androidTestImplementation(libs.androidx.junit)
        androidTestImplementation(libs.androidx.espresso.core)


    }
    apply(plugin = "com.google.gms.google-services")
