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
val cfr: Configuration by configurations.creating

// Dependencies

dependencies {
    // Minecraft & Fabric
    minecraft("com.mojang:minecraft:${property("minecraft_version")}")
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc:fabric-loader:${property("loader_version")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${property("fabric_version")}")

    // GraalVM (shaded)
    shadow(implementation("org.graalvm.sdk:graal-sdk:$graalVersion")!!)
    shadow(implementation("org.graalvm.truffle:truffle-api:$graalVersion")!!)
    shadow(implementation("org.graalvm.js:js-language:$graalVersion")!!)
    shadow(implementation("org.graalvm.js:js-scriptengine:$graalVersion")!!)

    // ByteBuddy (shaded)
    shadow(implementation("net.bytebuddy:byte-buddy:$bytebuddyVersion")!!)
    shadow(implementation("net.bytebuddy:byte-buddy-agent:$bytebuddyVersion")!!)

    // Lombok
    compileOnly("org.projectlombok:lombok:$lombokVersion")
    annotationProcessor("org.projectlombok:lombok:$lombokVersion")
    "clientCompileOnly"("org.projectlombok:lombok:$lombokVersion")
    "clientAnnotationProcessor"("org.projectlombok:lombok:$lombokVersion")

    // Source dependencies (for IDE browsing)
    sourceDeps("org.graalvm.sdk:graal-sdk:$graalVersion:sources@jar")
    sourceDeps("org.graalvm.truffle:truffle-api:$graalVersion:sources@jar")
    sourceDeps("org.graalvm.js:js-language:$graalVersion:sources@jar")
    sourceDeps("org.graalvm.js:js-scriptengine:$graalVersion:sources@jar")
    sourceDeps("net.bytebuddy:byte-buddy:$bytebuddyVersion:sources@jar")
    sourceDeps("net.bytebuddy:byte-buddy-agent:$bytebuddyVersion:sources@jar")

    // CFR decompiler
    cfr("org.benf:cfr:${property("cfrVersion")}")
}

// Java Toolchain

java {
    withSourcesJar()
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(25)
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

        sourceSets.forEach { sourceSet ->
            sourceSet.java.srcDirs.forEach { srcDir ->
                if (!srcDir.exists()) {
                    return@forEach
                }
                srcDir.walkTopDown()
                    .filter { it.isFile && it.name.endsWith(".java") }
                    .forEach { file ->
                        val content = file.readText(Charsets.UTF_8)
                        if (content.startsWith(firstHeaderLine)) {
                            return@forEach
                        }
                        println("Adding license header to: ${file.path}")
                        file.writeText(headerContent + "\n\n" + content, Charsets.UTF_8)
                    }
            }
        }
    }
}

// Resource Processing

listOf("processResources", "processClientResources").forEach { taskName ->
    tasks.named<ProcessResources>(taskName) {
        dependsOn(applyLicenseHeaders)
        inputs.property("version", project.version)

        filesMatching("fabric.mod.json") {
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
    "License" to "See $archivesBaseName-LICENSE.txt"
)

tasks.jar {
    from("LICENSE") {
        into("")
        rename { "$archivesBaseName-LICENSE.txt" }
    }

    manifest {
        attributes(manifestAttributes())
    }
}

tasks.shadowJar {
    archiveClassifier.set("dev")
    configurations = listOf(project.configurations["shadow"])
    from(sourceSets["client"].output)

    from("LICENSE") {
        into("META-INF")
        rename { "$archivesBaseName-LICENSE.txt" }
    }

    mergeServiceFiles()
    relocate("org.graalvm", "net.me.libs.graalvm")
    relocate("net.bytebuddy", "net.me.libs.bytebuddy")

    manifest {
        attributes(manifestAttributes())
    }
}

val shadowLibsJar by tasks.registering(ShadowJar::class) {
    archiveClassifier.set("libs-only")
    configurations = listOf(project.configurations["shadow"])
    dependencies {
        include { it.moduleGroup == "org.graalvm.sdk" && it.moduleName == "graal-sdk" }
        include { it.moduleGroup == "org.graalvm.truffle" && it.moduleName == "truffle-api" }
        include { it.moduleGroup == "org.graalvm.js" && it.moduleName == "js-language" }
        include { it.moduleGroup == "org.graalvm.js" && it.moduleName == "js-scriptengine" }
        include { it.moduleGroup == "net.bytebuddy" && it.moduleName == "byte-buddy" }
        include { it.moduleGroup == "net.bytebuddy" && it.moduleName == "byte-buddy-agent" }
    }
    from(files())
    mergeServiceFiles()
    relocate("org.graalvm", "net.me.libs.graalvm")
    relocate("net.bytebuddy", "net.me.libs.bytebuddy")
}

tasks.named<RemapJarTask>("remapJar") {
    dependsOn(tasks.named("shadowJar"))
    inputFile.set(tasks.named<ShadowJar>("shadowJar").get().archiveFile)

    from("LICENSE") {
        into("META-INF")
        rename { "$archivesBaseName-LICENSE.txt" }
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
    group = "help"
    description = "Deletes unpacked sources in libs-src/"
    delete(unpackedSourcesDir)
}

val unpackSources by tasks.registering(UnpackSourcesTask::class) {
    group = "help"
    description = "Unpacks library, Minecraft, and Fabric sources into libs-src/"
    dependsOn(cleanSources)
    sourceDeps.from(configurations.named("sourceDeps"))
    cfrClasspath.from(configurations.named("cfr"))
    outputDir.set(unpackedSourcesDir)
    minecraftCacheDir.set(minecraftCacheDirProvider)
    fabricCacheDir.set(fabricCacheDirProvider)
}
