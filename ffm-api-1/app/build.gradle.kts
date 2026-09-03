import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform;

plugins {
    application
}

repositories {
    mavenCentral()
}

dependencies {
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(26)
    }
}

val cargoExecutable = System.getProperty("user.home") +
    if (DefaultNativePlatform.getCurrentOperatingSystem().isWindows) {
        "\\.cargo\\bin\\cargo.exe"
    } else {
        "/.cargo/bin/cargo"
    }
val cargoTargetDir = layout.buildDirectory.dir("rust/math_lib/target/").get().asFile

application {
    mainClass = "org.example.Main"
    applicationDefaultJvmArgs = listOf(
        "--enable-native-access=ALL-UNNAMED")
}

tasks.register<Exec>("cargoBuild") {
    group = "rust"
    workingDir = layout.projectDirectory.dir("src/main/rust/math_lib").asFile
    description = "Builds the Rust library using Cargo"
    commandLine = listOf(cargoExecutable, "build", "--release",
        "--target-dir", cargoTargetDir.absolutePath)
}

tasks.named<JavaExec>("run") {
    dependsOn("cargoBuild")
    val rustLibDir = File(cargoTargetDir, "release").absolutePath
    environment("DYLD_LIBRARY_PATH", rustLibDir) // macOS
    environment("LD_LIBRARY_PATH", rustLibDir) // Linux
    environment("PATH", rustLibDir) // Windows
}
