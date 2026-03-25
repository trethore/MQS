import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import net.fabricmc.loom.task.RemapJarTask
import org.gradle.api.file.Directory
import org.gradle.api.tasks.Delete
import tytoo.myqolscripts.UnpackSourcesTask
import java.time.Year
import java.util.Date

// --- Plugins ---

plugins {
    id("fabric-loom")
    id("com.github.johnrengelman.shadow") version "8.1.1"
    `maven-publish`
}

// --- Project Properties ---

version = property("mod_version")!!
group = property("maven_group")!!

val archivesBaseName = property("archives_base_name") as String
base.archivesName.set(archivesBaseName)

val graalVersion: String by project
val bytebuddyVersion: String by project
val lombokVersion: String by project

// --- Constants ---

val shadowConfigName = "shadow"
val sourceDepsConfigName = "sourceDeps"
val licenseArchiveName = "$archivesBaseName-LICENSE.txt"

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

// --- Configurations ---

val sourceDeps: Configuration by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
    isTransitive = false
}

// --- Repositories ---

repositories {
    mavenCentral()
    maven { url = uri("https://packages.graalvm.org/maven") }
}

// --- Loom & Environment ---

loom {
    splitEnvironmentSourceSets()
    mods {
        register("myqolscripts") {
            sourceSet(sourceSets["client"])
        }
    }
}

java {
    withSourcesJar()
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(21)
}

// --- Dependencies ---

dependencies {
    // Minecraft & Fabric
    minecraft("com.mojang:minecraft:${property("minecraft_version")}")
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc:fabric-loader:${property("loader_version")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${property("fabric_version")}")

    // Graphene UI
    modImplementation("io.github.trethore:graphene-ui:${property("graphene_version")}")
    sourceDeps("io.github.trethore:graphene-ui:${property("graphene_version")}:sources@jar")

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

// --- Tasks ---

// 1. License Headers
val applyLicenseHeaders by tasks.registering {
    doLast {
        val headerFile = file("HEADER")
        if (!headerFile.exists()) {
            println("HEADER file not found. Skipping license header application.")
            return@doLast
        }

        val headerContent = headerFile.readText(Charsets.UTF_8).replace("<year>", Year.now().value.toString())
        val firstHeaderLine = headerContent.substringBefore('\n')

        sourceSets.asSequence()
            .flatMap { it.java.srcDirs.asSequence() }
            .filter { it.exists() }
            .flatMap { it.walkTopDown() }
            .filter { it.isFile && it.extension == "java" }
            .forEach { sourceFile ->
                val content = sourceFile.readText(Charsets.UTF_8)
                if (!content.startsWith(firstHeaderLine)) {
                    println("Adding license header to: ${sourceFile.path}")
                    sourceFile.writeText("$headerContent\n\n$content", Charsets.UTF_8)
                }
            }
    }
}

// 2. Resource Processing
listOf("processResources", "processClientResources").forEach { taskName ->
    tasks.named<ProcessResources>(taskName) {
        dependsOn(applyLicenseHeaders)
        inputs.property("version", project.version)

        filesMatching("fabric.mod.json") {
            expand("version" to project.version)
        }
    }
}

// 3. Packaging
val sharedManifestAttributes = mapOf(
    "Implementation-Title" to project.name,
    "Implementation-Version" to project.version,
    "Built-By" to System.getProperty("user.name"),
    "Built-Date" to Date(),
    "License" to "See $licenseArchiveName"
)

tasks.jar {
    from("LICENSE") {
        into("")
        rename { licenseArchiveName }
    }
    manifest { attributes(sharedManifestAttributes) }
}

val shadowJarProvider = tasks.named<ShadowJar>("shadowJar") {
    archiveClassifier.set("dev")
    configurations = listOf(project.configurations[shadowConfigName])
    from(sourceSets["client"].output)

    from("LICENSE") {
        into("META-INF")
        rename { licenseArchiveName }
    }

    mergeServiceFiles()
    relocate("org.graalvm", "net.me.libs.graalvm")
    relocate("net.bytebuddy", "net.me.libs.bytebuddy")

    manifest { attributes(sharedManifestAttributes) }
}

val shadowLibsJar by tasks.registering(ShadowJar::class) {
    archiveClassifier.set("libs-only")
    configurations = listOf(project.configurations[shadowConfigName])
    dependencies {
        shadedModules.forEach { (moduleGroup, moduleName) ->
            include { it.moduleGroup == moduleGroup && it.moduleName == moduleName }
        }
    }
    from(files())
    mergeServiceFiles()
    relocate("org.graalvm", "net.me.libs.graalvm")
    relocate("net.bytebuddy", "net.me.libs.bytebuddy")
}

tasks.named<RemapJarTask>("remapJar") {
    dependsOn(shadowJarProvider)
    inputFile.set(shadowJarProvider.flatMap { it.archiveFile })

    from("LICENSE") {
        into("META-INF")
        rename { licenseArchiveName }
    }
}

// --- Source Browsing Helpers ---

val unpackedSourcesDir: Directory = layout.projectDirectory.dir("references")

val cleanSources by tasks.registering(Delete::class) {
    group = "help"
    description = "Deletes unpacked sources in references/"
    delete(unpackedSourcesDir)
}

val unpackSources by tasks.registering(UnpackSourcesTask::class) {
    group = "help"
    description = "Unpacks library, Minecraft, and Fabric sources into references/"
    dependsOn(cleanSources)
    sourceDeps.from(configurations.named(sourceDepsConfigName))
    outputDir.set(unpackedSourcesDir)
    minecraftCacheDir.set(layout.projectDirectory.dir(".gradle/loom-cache/minecraftMaven"))
    fabricCacheDir.set(layout.projectDirectory.dir(".gradle/loom-cache/remapped_mods/remapped/net/fabricmc/fabric-api"))
}

// --- Publishing ---

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
