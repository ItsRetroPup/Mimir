import java.util.Properties

plugins {
    id("com.android.application")
    // The Flutter Gradle Plugin must be applied after the Android and Kotlin Gradle plugins.
    id("dev.flutter.flutter-gradle-plugin")
    id("com.chaquo.python")
}

android {
    namespace = "pup.app.mimir"
    compileSdk = flutter.compileSdkVersion
    ndkVersion = flutter.ndkVersion

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    defaultConfig {
        // TODO: Specify your own unique Application ID (https://developer.android.com/studio/build/application-id.html).
        applicationId = "pup.app.mimir"
        // You can update the following values to match your application needs.
        // For more information, see: https://flutter.dev/to/review-gradle-config.
        minSdk = 29
        targetSdk = flutter.targetSdkVersion
        // Uses the version code from pubspec.yaml. When using split APKs, 1000 * ABI_VERSION
        // is added automatically by Flutter. (https://developer.android.com/studio/build/configure-apk-splits#configure-APK-versions)
        // You can force using the value of versionCode by specifying the `-P force-version-code-ignoring-abi=true`
        // flag during build.
        versionCode = flutter.versionCode
        versionName = flutter.versionName
        ndk { abiFilters += "arm64-v8a" }
    }

    sourceSets {
        getByName("main").jniLibs.srcDir("../../../app/src/main/jniLibs")
        getByName("main").assets.srcDir("../../../app/src/main/assets")
        getByName("main").res.srcDir(layout.buildDirectory.dir("generated/mimir-icons"))
    }
    packaging { jniLibs { useLegacyPackaging = true } }

    val signingFile = rootProject.file("../../keystore.properties")
    if (signingFile.exists()) {
        val properties = Properties().apply { signingFile.inputStream().use(::load) }
        signingConfigs.create("mimirRelease") {
            val configured = java.io.File(properties.getProperty("storeFile"))
            storeFile = if (configured.isAbsolute) configured else rootProject.file("../../${configured.path}")
            storePassword = properties.getProperty("storePassword")
            keyAlias = properties.getProperty("keyAlias")
            keyPassword = properties.getProperty("keyPassword")
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("mimirRelease")
            isMinifyEnabled = false
        }
    }
}

chaquopy {
    sourceSets { getByName("main").srcDir("../../../app/src/main/python") }
    defaultConfig {
        version = "3.13"
        pip {
            install("nsz==5.0.0")
            install("zstandard==0.23.0")
            install("pycryptodome==3.21.0")
        }
    }
}

dependencies { implementation("androidx.documentfile:documentfile:1.0.1") }

tasks.matching { it.name == "preReleaseBuild" }.configureEach {
    doFirst { require(rootProject.file("../../keystore.properties").exists()) { "Release upgrades require the existing Mimir signing identity in keystore.properties." } }
}

val copyMimirIcons by tasks.registering(Sync::class) {
    from("../../../app/src/main/res") { include("mipmap-*/mimir_launcher_m.png") }
    into(layout.buildDirectory.dir("generated/mimir-icons"))
}
tasks.matching { it.name == "preBuild" }.configureEach { dependsOn(copyMimirIcons) }

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}

flutter {
    source = "../.."
}
