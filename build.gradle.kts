val pluginName = "1MB-Boosters"
val pluginVersion = "1.2.4"
val buildNumber = "036"
val targetJavaVersion = 25
val compilePaperApiVersion = "26.1.2"
val declaredApiCompatibilityVersion = "1.21.11"
val pluginYamlApiVersion = "1.21.11"
val artifactFileName = "${pluginName}-v${pluginVersion}-${buildNumber}-j${targetJavaVersion}-${declaredApiCompatibilityVersion}.jar"

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
    compileOnly("io.papermc.paper:paper-api:${compilePaperApiVersion}.build.+")
    compileOnly("me.clip:placeholderapi:2.11.6")
}

tasks.processResources {
    filteringCharset = "UTF-8"
    filesMatching(listOf("plugin.yml", "build-info.properties")) {
        expand(
            "pluginName" to pluginName,
            "version" to pluginVersion,
            "buildNumber" to buildNumber,
            "compilePaperApiVersion" to compilePaperApiVersion,
            "declaredApiCompatibilityVersion" to declaredApiCompatibilityVersion,
            "pluginYamlApiVersion" to pluginYamlApiVersion,
            "targetJavaVersion" to targetJavaVersion,
            "artifactFileName" to artifactFileName
        )
    }
}

tasks.jar {
    archiveFileName.set(artifactFileName)
}
