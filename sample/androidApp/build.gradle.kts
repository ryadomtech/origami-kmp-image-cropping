plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

android {
    namespace = "tech.ryadom.origami.sample.android"
    compileSdk = 36

    defaultConfig {
        applicationId = "tech.ryadom.origami.sample.android"
        minSdk = 23
        targetSdk = 36
        versionCode = 102
        versionName = "1.0.2"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
}

dependencies {
    implementation(libs.activityCompose)
    implementation(projects.sample.common)
}