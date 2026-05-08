plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

android {
    namespace = "tech.ryadom.origami.sample.android"
    compileSdk = 37

    defaultConfig {
        applicationId = "tech.ryadom.origami.sample.android"
        minSdk = 23
        targetSdk = 37
        versionCode = 110
        versionName = "1.1.0"
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