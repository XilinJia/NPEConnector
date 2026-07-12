// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.library") version "9.2.1"
    id("org.jetbrains.kotlin.plugin.parcelize") version "2.4.0"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.4.0"
    id("maven-publish")
}

kotlin { jvmToolchain(21) }

android {
    namespace = "ac.roma.npeconnector"

    compileSdk = 37
    buildToolsVersion = "37.0.0"

    defaultConfig {
        minSdk = 26
        consumerProguardFiles("consumer-rules.pro")
    }

//      sourceSets {
//          getByName("main") {
//              kotlin.directories.add("../../PodciniLib/src/main/kotlin")
//              aidl.directories.add("../../PodciniLib/src/main/aidl")
//          }
//      }

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
    val kotlinVersion by extra("2.4.0")
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.19.0")
    implementation("androidx.compose.runtime:runtime:1.11.4")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.8.0")

    implementation("io.ktor:ktor-http:3.5.1")
    implementation("io.ktor:ktor-client-core:3.5.1")
    implementation("io.ktor:ktor-client-okhttp:3.5.1")
    implementation("io.ktor:ktor-client-cio:3.5.1")
    implementation("io.ktor:ktor-utils:3.5.1")

    implementation("com.github.XilinJia:PodciniLib:1.0.9")

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs_nio:2.1.5")
    implementation("com.github.teamnewpipe:NewPipeExtractor:v0.26.3")
    implementation("com.github.TeamNewPipe:nanojson:e9d656ddb49a412a5a0a5d5ef20ca7ef09549996")
    implementation("io.reactivex.rxjava3:rxjava:3.1.12")
    implementation("io.reactivex.rxjava3:rxandroid:3.0.2")
}

publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = "com.github.xilinjia"
            artifactId = "NPEConnector"
            version = "1.0.2"
            afterEvaluate {
                from(components["release"])
            }
        }
    }
}
