import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    id("com.android.application")
    id("kotlin-multiplatform")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

kotlin {
    androidTarget()

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    jvm("desktop")

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        outputModuleName = "OrigamiSample"
        browser {
            val rootDirPath = project.rootDir.path
            val projectDirPath = project.projectDir.path
            commonWebpackConfig {
                outputFileName = "OrigamiSample.js"
                devServer = (devServer ?: KotlinWebpackConfig.DevServer()).apply {
                    static(rootDirPath)
                    static(projectDirPath)
                }
            }
        }

        binaries.executable()
    }

    sourceSets {
        androidMain.dependencies {
            implementation("androidx.activity:activity-compose:1.11.0")
        }

        commonMain.dependencies {
            implementation(projects.lib)

            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)
            implementation(compose.material3)
            implementation(compose.components.resources)

            implementation("co.touchlab:kermit:2.0.8")
        }

        named("desktopMain") {
            dependencies {
                implementation(compose.desktop.currentOs)
            }
        }
    }
}

android {
    namespace = "tech.ryadom.origami.sample"
    compileSdk = 36

    defaultConfig {
        applicationId = "tech.ryadom.origami.sample"
        minSdk = 24
        targetSdk = 36
        versionCode = 101
        versionName = "1.0.1"
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

compose.desktop {
    application {
        mainClass = "tech.ryadom.origami.sample.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "tech.ryadom.origami.sample"
            packageVersion = "1.0.1"
        }
    }
}

afterEvaluate {
    tasks {
        val configureJs: Task.() -> Unit = {
            dependsOn(named("wasmJsDevelopmentExecutableCompileSync"))
            dependsOn(named("wasmJsProductionExecutableCompileSync"))
            dependsOn(named("wasmJsTestTestDevelopmentExecutableCompileSync"))
        }

        named("wasmJsBrowserProductionWebpack").configure(configureJs)
    }
}

tasks.withType<JavaCompile>().configureEach {
    sourceCompatibility = JavaVersion.VERSION_11.toString()
    targetCompatibility = JavaVersion.VERSION_11.toString()
}

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions.jvmTarget = JvmTarget.JVM_11
}