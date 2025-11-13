import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.publishing)
}

kotlin {
    jvm("desktop")

    js(IR) {
        nodejs()
        browser()
        binaries.executable()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        nodejs()
        binaries.executable()
    }

    androidTarget {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
                }
            }
        }

        publishLibraryVariants("release")
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)
        }
    }
}

android {
    namespace = "tech.ryadom.origami"
    compileSdk = 36

    defaultConfig {
        minSdk = 23
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

mavenPublishing {
    publishToMavenCentral(automaticRelease = true)

    signAllPublications()

    coordinates(
        groupId = "tech.ryadom",
        artifactId = "origami",
        version = "1.0.1"
    )

    pom {
        name.set("Origami")
        description.set("Simple image cropping tool for Compose Multiplatform")
        inceptionYear.set("2025")
        url.set("https://github.com/ryadomtech/origami")

        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("http://www.apache.org/licenses/LICENSE-2.0")
                distribution.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }

        developers {
            developer {
                id.set("adkozlovskiy")
                name.set("Alexey Kozlovsky")
                email.set("adkozlovskiy@gmail.com")
            }
        }

        scm {
            url.set("https://github.com/ryadomtech/origami")
            connection.set("scm:git:git://github.com/ryadomtech/origami.git")
            developerConnection.set("scm:git:ssh://git@github.com/ryadomtech/origami.git")
        }
    }
}