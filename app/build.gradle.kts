plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.zaystudio.zayclicker"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.zaystudio.zayclicker"
        minSdk = 26
        targetSdk = 32
        versionCode = 3
        versionName = "0.3.0-multitouch"
        ndk { abiFilters += listOf("arm64-v8a") }
        externalNativeBuild {
            cmake { cppFlags += listOf("-std=c++17") }
        }
    }

    externalNativeBuild {
        cmake { path = file("src/main/cpp/CMakeLists.txt"); version = "3.22.1" }
    }

    sourceSets["main"].assets.srcDir(layout.buildDirectory.dir("generated/zayNativeAssets"))

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

val stageNativeTouch = tasks.register("stageNativeTouch") {
    dependsOn("externalNativeBuildDebug")
    outputs.file(layout.buildDirectory.file("generated/zayNativeAssets/zaytouch"))
    doLast {
        val cxxDir = layout.projectDirectory.dir(".cxx/Debug").asFile
        val candidates = fileTree(cxxDir) { include("**/zaybin/zaytouch") }.files
        if (candidates.isEmpty()) {
            throw GradleException("Native zaytouch executable tidak ditemukan di ${cxxDir.absolutePath}")
        }
        val destination = layout.buildDirectory.file("generated/zayNativeAssets/zaytouch").get().asFile
        destination.parentFile.mkdirs()
        candidates.first().copyTo(destination, overwrite = true)
        destination.setReadable(true, false)
    }
}

tasks.matching { it.name == "mergeDebugAssets" }.configureEach {
    dependsOn(stageNativeTouch)
}
