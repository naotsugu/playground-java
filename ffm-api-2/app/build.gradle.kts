import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform;

plugins {
    application
}

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

application {
    mainClass = "org.example.Main"
    applicationDefaultJvmArgs = listOf("--enable-native-access=ALL-UNNAMED")
}

val cargoExecutable = System.getProperty("user.home") + "/.cargo/bin/cargo"
val cargoTargetDir = layout.buildDirectory.dir("rust/math_lib/").get().asFile

val os   = DefaultNativePlatform.getCurrentOperatingSystem()
val arch = DefaultNativePlatform.getCurrentArchitecture()

tasks.register<Copy>("dljextract") {
    description = "download jextract"
    val url = when {
        os.isMacOsX  && arch.isArm64 -> "https://download.java.net/java/early_access/jextract/25/2/openjdk-25-jextract+2-4_macos-aarch64_bin.tar.gz"
        os.isMacOsX  && arch.isAmd64 -> "https://download.java.net/java/early_access/jextract/25/2/openjdk-25-jextract+2-4_macos-x64_bin.tar.gz"
        os.isLinux   && arch.isArm64 -> "https://download.java.net/java/early_access/jextract/25/2/openjdk-25-jextract+2-4_linux-aarch64_bin.tar.gz"
        os.isLinux   && arch.isAmd64 -> "https://download.java.net/java/early_access/jextract/25/2/openjdk-25-jextract+2-4_linux-x64_bin.tar.gz"
        os.isWindows && arch.isAmd64 -> "https://download.java.net/java/early_access/jextract/25/2/openjdk-25-jextract+2-4_windows-x64_bin.tar.gz"
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

val jextractOutputDir = layout.buildDirectory.dir("generated/main/java")
sourceSets {
    main {
        java {
            srcDir(jextractOutputDir)
        }
    }
}

tasks.register<Exec>("jextract") {
    dependsOn("dljextract")
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
        layout.projectDirectory.dir("src/main/rust/math_lib/src/math_lib.h").asFile.absolutePath,
        "--output", jextractOutputDir.get().asFile.absolutePath,
        "-t", "com.example.math_lib",
        "-l", "math_lib"
    )
}

tasks.named("compileJava") {
    dependsOn("jextract")
}

tasks.register<Exec>("cargoBuild") {
    description = "Builds the Rust library using Cargo"
    group = "rust"
    workingDir = layout.projectDirectory.dir("src/main/rust/math_lib").asFile

    inputs.dir("$workingDir/src").withPropertyName("rustSourceDir")
    inputs.files("$workingDir/Cargo.toml", "$workingDir/Cargo.lock").withPropertyName("cargoToml")
    outputs.dir(cargoTargetDir).withPropertyName("cargoTargetDir")

    commandLine = listOf(cargoExecutable, "build", "--release",
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
