import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import net.fabricmc.loom.task.RemapJarTask
import org.gradle.api.file.Directory
import org.gradle.api.tasks.Delete
import tytoo.myqolscripts.UnpackSourcesTask
import java.time.Year
import java.util.Date

// Plugins

plugins {
    id("fabric-loom")
    id("com.github.johnrengelman.shadow") version "8.1.1"
    `maven-publish`
}

// Project Metadata

version = property("mod_version")!!
group = property("maven_group")!!

val archivesBaseName: String = property("archives_base_name") as String
val sourceDepsConfigurationName = "sourceDeps"
val shadowConfigurationName = "shadow"
val shadowJarTaskName = "shadowJar"
val helpTaskGroup = "help"
val licenseFileName = "LICENSE"
val licenseArchiveName = "$archivesBaseName-LICENSE.txt"
val fabricModMetadataFile = "fabric.mod.json"
val processResourceTaskNames = listOf("processResources", "processClientResources")
val javaSourceExtension = "java"
val graalRelocationFrom = "org.graalvm"
val graalRelocationTo = "net.me.libs.graalvm"
val byteBuddyRelocationFrom = "net.bytebuddy"
val byteBuddyRelocationTo = "net.me.libs.bytebuddy"

val graalModules = listOf(
    "org.graalvm.sdk" to "graal-sdk",
    "org.graalvm.truffle" to "truffle-api",
    "org.graalvm.js" to "js-language",
    "org.graalvm.js" to "js-scriptengine"
)
val byteBuddyModules = listOf(
    "net.bytebuddy" to "byte-buddy",
    "net.bytebuddy" to "byte-buddy-agent"
)
val shadedModules = graalModules + byteBuddyModules

base {
    archivesName.set(archivesBaseName)
}

// Dependency Versions (from gradle.properties)

val graalVersion: String by project
val bytebuddyVersion: String by project
val lombokVersion: String by project

// Repositories

repositories {
    mavenCentral()
    maven { url = uri("https://packages.graalvm.org/maven") }
}

// Loom Configuration

loom {
    splitEnvironmentSourceSets()

    mods {
        register("myqolscripts") {
            sourceSet(sourceSets["client"])
        }
    }
}

// Configurations

val sourceDeps: Configuration by configurations.creating

// Dependencies

dependencies {
    // Minecraft & Fabric
    minecraft("com.mojang:minecraft:${property("minecraft_version")}")
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc:fabric-loader:${property("loader_version")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${property("fabric_version")}")

    // GraalVM (shaded)
    graalModules.forEach { (moduleGroup, moduleName) ->
        val coordinate = "$moduleGroup:$moduleName:$graalVersion"
        shadow(implementation(coordinate)!!)
        sourceDeps("$coordinate:sources@jar")
    }

    // ByteBuddy (shaded)
    byteBuddyModules.forEach { (moduleGroup, moduleName) ->
        val coordinate = "$moduleGroup:$moduleName:$bytebuddyVersion"
        shadow(implementation(coordinate)!!)
        sourceDeps("$coordinate:sources@jar")
    }

    // Lombok
    compileOnly("org.projectlombok:lombok:$lombokVersion")
    annotationProcessor("org.projectlombok:lombok:$lombokVersion")
    "clientCompileOnly"("org.projectlombok:lombok:$lombokVersion")
    "clientAnnotationProcessor"("org.projectlombok:lombok:$lombokVersion")

}

// Java Toolchain

java {
    withSourcesJar()
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(21)
}

// License Headers

val applyLicenseHeaders by tasks.registering {
    doLast {
        val headerFile = file("HEADER")
        if (!headerFile.exists()) {
            println("HEADER file not found. Skipping license header application.")
            return@doLast
        }

        val headerContent = headerFile.readText(Charsets.UTF_8)
            .replace("<year>", Year.now().value.toString())
        val firstHeaderLine = headerContent.lines().first()

        val javaSourceFiles = sourceSets.asSequence()
            .flatMap { sourceSet -> sourceSet.java.srcDirs.asSequence() }
            .filter { srcDir -> srcDir.exists() }
            .flatMap { srcDir -> srcDir.walkTopDown() }
            .filter { sourceFile -> sourceFile.isFile && sourceFile.extension == javaSourceExtension }

        javaSourceFiles.forEach { sourceFile ->
            val content = sourceFile.readText(Charsets.UTF_8)
            if (content.startsWith(firstHeaderLine)) {
                return@forEach
            }
            println("Adding license header to: ${sourceFile.path}")
            sourceFile.writeText(headerContent + "\n\n" + content, Charsets.UTF_8)
        }
    }
}

// Resource Processing

processResourceTaskNames.forEach { taskName ->
    tasks.named<ProcessResources>(taskName) {
        dependsOn(applyLicenseHeaders)
        inputs.property("version", project.version)

        filesMatching(fabricModMetadataFile) {
            expand("version" to project.version)
        }
    }
}

// JAR Packaging

fun manifestAttributes(): Map<String, Any> = mapOf(
    "Implementation-Title" to project.name,
    "Implementation-Version" to project.version,
    "Built-By" to System.getProperty("user.name"),
    "Built-Date" to Date(),
    "License" to "See $licenseArchiveName"
)

tasks.jar {
    from(licenseFileName) {
        into("")
        rename { licenseArchiveName }
    }

    manifest {
        attributes(manifestAttributes())
    }
}

tasks.shadowJar {
    archiveClassifier.set("dev")
    configurations = listOf(project.configurations[shadowConfigurationName])
    from(sourceSets["client"].output)

    from(licenseFileName) {
        into("META-INF")
        rename { licenseArchiveName }
    }

    mergeServiceFiles()
    relocate(graalRelocationFrom, graalRelocationTo)
    relocate(byteBuddyRelocationFrom, byteBuddyRelocationTo)

    manifest {
        attributes(manifestAttributes())
    }
}

val shadowLibsJar by tasks.registering(ShadowJar::class) {
    archiveClassifier.set("libs-only")
    configurations = listOf(project.configurations[shadowConfigurationName])
    dependencies {
        shadedModules.forEach { (moduleGroup, moduleName) ->
            include { it.moduleGroup == moduleGroup && it.moduleName == moduleName }
        }
    }
    from(files())
    mergeServiceFiles()
    relocate(graalRelocationFrom, graalRelocationTo)
    relocate(byteBuddyRelocationFrom, byteBuddyRelocationTo)
}

val shadowJarProvider = tasks.named<ShadowJar>(shadowJarTaskName)

tasks.named<RemapJarTask>("remapJar") {
    dependsOn(shadowJarProvider)
    inputFile.set(shadowJarProvider.flatMap { shadowJarTask -> shadowJarTask.archiveFile })

    from(licenseFileName) {
        into("META-INF")
        rename { licenseArchiveName }
    }
}

// Publishing

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = archivesBaseName
            from(components["java"])
        }
    }
    repositories {
        // Configure your publish repos here
    }
}

// Source Browsing Helpers

val unpackedSourcesDir: Directory = layout.projectDirectory.dir("libs-src")
val minecraftCacheDirProvider: Directory = layout.projectDirectory.dir(".gradle/loom-cache/minecraftMaven")
val fabricCacheDirProvider: Directory = layout.projectDirectory.dir(".gradle/loom-cache/remapped_mods/remapped/net/fabricmc/fabric-api")

val cleanSources by tasks.registering(Delete::class) {
    group = helpTaskGroup
    description = "Deletes unpacked sources in libs-src/"
    delete(unpackedSourcesDir)
}

val unpackSources by tasks.registering(UnpackSourcesTask::class) {
    group = helpTaskGroup
    description = "Unpacks library, Minecraft, and Fabric sources into libs-src/"
    dependsOn(cleanSources)
    sourceDeps.from(configurations.named(sourceDepsConfigurationName))
    outputDir.set(unpackedSourcesDir)
    minecraftCacheDir.set(minecraftCacheDirProvider)
    fabricCacheDir.set(fabricCacheDirProvider)
}
