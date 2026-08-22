import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsTargetDsl

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinMultiplatformLibrary)
    alias(libs.plugins.publishing)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    // Required: the manual dependsOn below otherwise opts this project out of the default
    // hierarchy and appleMain stops existing.
    applyDefaultHierarchyTemplate()

    explicitApi()

    jvmToolchain(21)
    jvm("desktop")

    js {
        nodejs()
        browser()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        // Node cannot fetch skiko's .wasm binary, so the wasm tests run in a browser.
        nodejs {
            testTask {
                enabled = false
            }
        }
        browser()
    }

    android {
        namespace = "tech.ryadom.origami"
        compileSdk = 37
        minSdk = 23

        withHostTest {}
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "Origami"
            isStatic = true
        }
    }

    sourceSets {
        // Desktop, Apple, JS and Wasm all render through skiko and share one image compressor.
        val skikoMain by creating {
            dependsOn(commonMain.get())
        }

        // configureEach, not named(): appleMain is materialized after this block runs.
        val skikoTargets = setOf("desktopMain", "appleMain", "jsMain", "wasmJsMain")
        configureEach {
            if (name in skikoTargets) {
                dependsOn(skikoMain)
            }
        }

        commonMain.dependencies {
            api(libs.composeRuntime)
            api(libs.composeFoundation)
            api(libs.composeUi)
            api(libs.composeRuntimeSaveable)

            implementation(libs.coroutinesCore)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.coroutinesTest)
        }

        // Bitmap level tests need the real Skia backend of the desktop target.
        named("desktopTest").dependencies {
            implementation(compose.desktop.currentOs)
        }
    }
}

mavenPublishing {
    publishToMavenCentral(automaticRelease = true)

    signAllPublications()

    coordinates(
        groupId = "tech.ryadom",
        artifactId = "origami",
        version = "2.0.0"
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
