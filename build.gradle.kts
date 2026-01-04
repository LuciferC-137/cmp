plugins {
    application
    java
    id("org.openjfx.javafxplugin") version "0.1.0"
}

group = "com.luciferc137.cmp"
version = "1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

javafx {
    version = "21"
    modules = listOf(
        "javafx.controls",
        "javafx.fxml",
        "javafx.media"
    )
}

application {
    mainClass.set("com.luciferc137.cmp.MainApp")
    applicationDefaultJvmArgs = listOf(
        "--add-modules=javafx.controls,javafx.fxml,javafx.media"
    )
}

tasks.named<JavaExec>("run") {

}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    implementation("org.xerial:sqlite-jdbc:3.45.1.0")
    implementation("org.jetbrains:annotations:24.0.1")

    // JSON for settings management
    implementation("com.google.code.gson:gson:2.10.1")

    // VLCJ for audio playback (alternative to JavaFX Media)
    implementation("uk.co.caprica:vlcj:4.8.2")

    // Support MP3 for Java Sound API (waveform extraction)
    implementation("com.googlecode.soundlibs:mp3spi:1.9.5.4")
    implementation("com.googlecode.soundlibs:jlayer:1.0.1.4")
    implementation("com.googlecode.soundlibs:tritonus-share:0.3.7.4")

    // JAudioTagger for reading metadata from various audio formats (MP3, M4A, FLAC, OGG, etc.)
    implementation("net.jthink:jaudiotagger:3.0.1")

    // SLF4J simple logger implementation
    implementation("org.slf4j:slf4j-simple:2.0.9")
}

tasks.test {
    useJUnitPlatform()
}