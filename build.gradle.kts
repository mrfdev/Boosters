val pluginName = "1MB-Boosters"
val pluginVersion = "1.2.1"
val buildNumber = "023"
val targetJavaVersion = 25
val targetMinecraftVersion = "1.21.11"
val artifactFileName = "${pluginName}-v${pluginVersion}-${buildNumber}-j${targetJavaVersion}-${targetMinecraftVersion}.jar"

plugins {
    java
}

group = "com.mrfdev"
version = pluginVersion

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(targetJavaVersion)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(targetJavaVersion))
    }
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:${targetMinecraftVersion}-R0.1-SNAPSHOT")
    compileOnly("me.clip:placeholderapi:2.11.6")
}

tasks.processResources {
    filteringCharset = "UTF-8"
    filesMatching(listOf("plugin.yml", "build-info.properties")) {
        expand(
            "pluginName" to pluginName,
            "version" to project.version,
            "buildNumber" to buildNumber,
            "targetMinecraftVersion" to targetMinecraftVersion,
            "targetJavaVersion" to targetJavaVersion,
            "artifactFileName" to artifactFileName
        )
    }
}

tasks.jar {
    archiveFileName.set(artifactFileName)
}
