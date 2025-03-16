plugins {
    application
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(23)
    }
}

application {
    mainClass = "org.example.App"
}

tasks.run.configure {
    standardInput = System.`in`
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
