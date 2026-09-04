import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform

plugins {
    application
}
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(26)
    }
}

application {
    mainClass = "org.example.Main"
    applicationDefaultJvmArgs = listOf("--enable-native-access=ALL-UNNAMED")
}

val cargoExecutable = System.getProperty("user.home") + "/.cargo/bin/cargo"
val cargoTargetDir = layout.buildDirectory.dir("rust/math_lib/").get().asFile

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
