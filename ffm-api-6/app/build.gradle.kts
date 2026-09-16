import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform

plugins {
    application
    id("org.openjfx.javafxplugin") version "0.1.0"
}

repositories {  mavenCentral() }

java.toolchain {
    languageVersion = JavaLanguageVersion.of(25)
}

application {
    mainClass = "org.example.Main"
    applicationDefaultJvmArgs = listOf(
        "--enable-native-access=ALL-UNNAMED",
        "--enable-native-access=javafx.graphics")
}
javafx {
    version = "25"
    modules("javafx.controls")
}

// -- cargo ---
val os   = DefaultNativePlatform.getCurrentOperatingSystem()
val arch = DefaultNativePlatform.getCurrentArchitecture()

val cargoExecutable = System.getProperty("user.home") + "/.cargo/bin/cargo"
val cargoTargetDir = layout.buildDirectory.dir("rust/rust_lib/").get().asFile
val rustProjectDir = layout.projectDirectory.dir("src/main/rust/rust_lib").asFile
val jextractBaseUrl = "https://download.java.net/java/early_access/jextract/25/2/"
val jextractOutputDir = layout.buildDirectory.dir("generated/main/java")


tasks.register<Exec>("cargoBuild") {
    description = "Builds the Rust library using Cargo"
    group = "rust"
    workingDir = rustProjectDir
    inputs.dir("$workingDir/src").withPropertyName("rustSourceDir")
    inputs.files("$workingDir/Cargo.toml", "$workingDir/Cargo.lock").withPropertyName("cargoToml")
    outputs.dir(cargoTargetDir).withPropertyName("cargoTargetDir")
    commandLine = listOf(cargoExecutable,
        "build", "--release",
        "--target-dir", cargoTargetDir.absolutePath)
}


tasks.named<JavaExec>("run") {
    dependsOn("cargoBuild")
    val rustLibDir = File(cargoTargetDir, "release").absolutePath
    val os = DefaultNativePlatform.getCurrentOperatingSystem()
    if (os.isMacOsX) {
        environment("DYLD_LIBRARY_PATH", rustLibDir)
    } else if (os.isWindows) {
        environment("PATH", rustLibDir)
    } else {
        environment("LD_LIBRARY_PATH", rustLibDir)
    }
}


tasks.register<Copy>("downloadJextract") {
    description = "download jextract"
    val url = when {
        os.isMacOsX  && arch.isArm64 -> "$jextractBaseUrl/openjdk-25-jextract+2-4_macos-aarch64_bin.tar.gz"
        os.isMacOsX  && arch.isAmd64 -> "$jextractBaseUrl/openjdk-25-jextract+2-4_macos-x64_bin.tar.gz"
        os.isLinux   && arch.isArm64 -> "$jextractBaseUrl/openjdk-25-jextract+2-4_linux-aarch64_bin.tar.gz"
        os.isLinux   && arch.isAmd64 -> "$jextractBaseUrl/openjdk-25-jextract+2-4_linux-x64_bin.tar.gz"
        os.isWindows && arch.isAmd64 -> "$jextractBaseUrl/openjdk-25-jextract+2-4_windows-x64_bin.tar.gz"
        else -> throw Error("Unsupported OS: $os, ARCH: $arch")
    }
    val tgz = layout.buildDirectory.file("jextract.tar.gz")
    tgz.get().asFile.apply {
        if (!exists()) {
            parentFile.mkdirs()
            uri(url).toURL().openStream().use { input ->
                outputStream().use { out -> input.copyTo(out) }
            }
        }
    }

    from(tarTree(resources.gzip(tgz.get())))
    into(layout.buildDirectory.dir("jextract"))
}


tasks.register<Exec>("jextract") {
    dependsOn("downloadJextract")
    group = "jextract"
    description = "Generates Java bindings from C header using jextract"

    doFirst {
        mkdir(jextractOutputDir.get())
    }

    inputs.dir("$workingDir/src").withPropertyName("rustSourceDir")
    inputs.files("$workingDir/Cargo.toml", "$workingDir/Cargo.lock").withPropertyName("cargoToml")
    outputs.dir(jextractOutputDir).withPropertyName("jextractOutputDir")

    commandLine(
        layout.buildDirectory.dir("jextract/jextract-25/bin/jextract").get().asFile.absolutePath + if (os.isWindows) ".bat" else "",
        layout.projectDirectory.dir("src/main/rust/rust_lib/src/rust_lib.h").asFile.absolutePath,
        "--output", jextractOutputDir.get().asFile.absolutePath,
        "-t", "com.example.rust_lib",
        "-l", "rust_lib"
    )
}

tasks.named("compileJava") {
    dependsOn("jextract")
}

sourceSets {
    main {
        java {
            srcDir(jextractOutputDir)
        }
    }
}
