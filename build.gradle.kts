// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.library") version "9.5.1"
    id("org.jetbrains.kotlin.plugin.parcelize") version "2.4.21"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.4.21"
    id("maven-publish")
}

kotlin { jvmToolchain(21) }

android {
    namespace = "ac.roma.npeconnector"

    compileSdk {
        version = release(37) {
            minorApiLevel = 2
        }
    }

    defaultConfig {
        minSdk = 26
        consumerProguardFiles("consumer-rules.pro")
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        aidl = true
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

buildscript {
    val kotlinVersion by extra("2.4.21")
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
    }
}

dependencies {
    implementation("androidx.collection:collection:1.7.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.8.0")

    implementation("io.ktor:ktor-http:3.6.1")
    implementation("io.ktor:ktor-client-core:3.6.1")
    implementation("io.ktor:ktor-client-okhttp:3.6.1")

   implementation("com.github.XilinJia:PodciniLib:1.1.8")

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs_nio:2.1.5")
    implementation("com.github.teamnewpipe:NewPipeExtractor:v0.27.1")
}

publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = "com.github.xilinjia"
            artifactId = "NPEConnector"
            version = "1.1.11"
            afterEvaluate {
                from(components["release"])
            }
        }
    }
}
