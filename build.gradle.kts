plugins {
    application
    java
    id("org.openjfx.javafxplugin") version "0.1.0"
}

group = "com.luciferc137.cmp"
version = "0.1.0"

val appName = "cmp"
val appDescription = "Custom Music Player - A modern music player for your local library"
val appVendor = "LuciferC137"

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
        "--add-modules=javafx.controls,javafx.fxml,javafx.media",
        "-Djdk.gtk.version=3"
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

// Create a fat JAR with all dependencies
tasks.register<Jar>("fatJar") {
    group = "build"
    description = "Creates a fat JAR with all dependencies"
    archiveClassifier.set("all")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    from(sourceSets.main.get().output)

    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get()
            .filter { it.name.endsWith("jar") }
            .map { zipTree(it) }
    })

    manifest {
        attributes["Main-Class"] = "com.luciferc137.cmp.MainApp"
    }
}

// Task to create a Linux application image using jpackage
tasks.register<Exec>("jpackageImage") {
    group = "distribution"
    description = "Creates a Linux application image using jpackage"
    dependsOn("fatJar")

    val outputDir = layout.buildDirectory.dir("jpackage").get().asFile
    val iconFile = file("packaging/linux/cmp.png")
    val inputDir = layout.buildDirectory.dir("libs").get().asFile
    val jarName = "cmp-${version}-all.jar"

    // Get JavaFX JARs from Gradle cache for module-path
    val javafxModulePath = configurations.runtimeClasspath.get()
        .filter { it.name.contains("javafx") && it.name.endsWith(".jar") }
        .joinToString(File.pathSeparator) { it.absolutePath }

    doFirst {
        outputDir.mkdirs()
        val jarFile = file("${inputDir.absolutePath}/$jarName")
        if (!jarFile.exists()) {
            throw GradleException("Fat JAR not found at ${jarFile.absolutePath}. Run './gradlew fatJar' first.")
        }
        println("Using JavaFX module path: $javafxModulePath")
    }

    commandLine(
        "jpackage",
        "--type", "app-image",
        "--name", appName,
        "--app-version", version.toString(),
        "--vendor", appVendor,
        "--description", appDescription,
        "--input", inputDir.absolutePath,
        "--main-jar", jarName,
        "--main-class", "com.luciferc137.cmp.MainApp",
        "--dest", outputDir.absolutePath,
        "--icon", iconFile.absolutePath,
        // Use module-path to include JavaFX modules
        "--module-path", javafxModulePath,
        "--add-modules", "javafx.controls,javafx.fxml,javafx.media,javafx.graphics,javafx.base,java.logging,java.sql,java.naming,jdk.unsupported",
        "--java-options", "--add-opens=javafx.graphics/javafx.scene=ALL-UNNAMED",
        "--java-options", "--add-opens=javafx.base/com.sun.javafx.runtime=ALL-UNNAMED",
        "--java-options", "-Djdk.gtk.version=3"
    )
}

// Task to create a native installer package (.deb or .rpm) using jpackage
tasks.register<Exec>("jpackage") {
    group = "distribution"
    description = "Creates a native Linux installer package (.deb by default, use -PinstallerType=rpm for RPM)"
    dependsOn("fatJar")

    val outputDir = layout.buildDirectory.dir("jpackage").get().asFile
    val iconFile = file("packaging/linux/cmp.png")
    val resourceDir = file("packaging/linux/resources")
    val installerType = project.findProperty("installerType")?.toString() ?: "deb"
    val inputDir = layout.buildDirectory.dir("libs").get().asFile
    val jarName = "cmp-${version}-all.jar"

    // Get JavaFX JARs from Gradle cache for module-path
    val javafxModulePath = configurations.runtimeClasspath.get()
        .filter { it.name.contains("javafx") && it.name.endsWith(".jar") }
        .joinToString(File.pathSeparator) { it.absolutePath }

    doFirst {
        outputDir.mkdirs()
        // Verify the JAR exists
        val jarFile = file("${inputDir.absolutePath}/$jarName")
        if (!jarFile.exists()) {
            throw GradleException("Fat JAR not found at ${jarFile.absolutePath}. Run './gradlew fatJar' first.")
        }
        println("Using JavaFX module path: $javafxModulePath")
    }

    commandLine(
        "jpackage",
        "--type", installerType,
        "--name", appName,
        "--app-version", version.toString(),
        "--vendor", appVendor,
        "--description", appDescription,
        "--input", inputDir.absolutePath,
        "--main-jar", jarName,
        "--main-class", "com.luciferc137.cmp.MainApp",
        "--dest", outputDir.absolutePath,
        "--icon", iconFile.absolutePath,
        "--resource-dir", resourceDir.absolutePath,
        "--linux-shortcut",
        "--linux-menu-group", "AudioVideo;Audio;Player",
        "--linux-app-category", "audio",
        "--linux-package-name", "cmp",
        // Use module-path to include JavaFX modules
        "--module-path", javafxModulePath,
        "--add-modules", "javafx.controls,javafx.fxml,javafx.media,javafx.graphics,javafx.base,java.logging,java.sql,java.naming,jdk.unsupported",
        "--java-options", "--add-opens=javafx.graphics/javafx.scene=ALL-UNNAMED",
        "--java-options", "--add-opens=javafx.base/com.sun.javafx.runtime=ALL-UNNAMED",
        "--java-options", "-Djdk.gtk.version=3"
    )
}

// Task to create a .desktop file for Linux integration
tasks.register("installDesktop") {
    group = "distribution"
    description = "Creates desktop integration files"
    dependsOn("jpackageImage")

    doLast {
        val desktopFile = file("${System.getProperty("user.home")}/.local/share/applications/cmp.desktop")
        val appImageDir = layout.buildDirectory.dir("jpackage/$appName").get().asFile

        desktopFile.writeText("""
            [Desktop Entry]
            Name=CMP
            Comment=$appDescription
            Exec=${appImageDir.absolutePath}/bin/$appName
            Icon=${appImageDir.absolutePath}/lib/cmp.png
            Terminal=false
            Type=Application
            Categories=AudioVideo;Audio;Player;
            Keywords=music;player;audio;mp3;flac;ogg;
            StartupWMClass=cmp
        """.trimIndent())

        println("Desktop file created at: ${desktopFile.absolutePath}")
        println("Application installed at: ${appImageDir.absolutePath}")
    }
}
