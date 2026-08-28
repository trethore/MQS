plugins {
  alias(libs.plugins.fabric.loom.remap)
  `maven-publish`
}

val javaVersion = JavaLanguageVersion.of(21)
val targetMinecraftVersion = libs.versions.minecraft.v12111.get()
val loaderVersion = libs.versions.fabric.loader.v12111.get()
val fabricApiVersion = libs.versions.fabric.api.v12111.get()

base {
  archivesName = rootProject.name
}

loom {
  runs {
    named("client") {
      displayName.set("Minecraft Client (Fabric $targetMinecraftVersion)")
      appendProjectPathToDisplayName.set(false)
      generateRunConfig.set(true)
      runDirectory.set(layout.projectDirectory.dir("run/client"))
    }

    named("server") {
      displayName.set("Minecraft Server (Fabric $targetMinecraftVersion)")
      appendProjectPathToDisplayName.set(false)
      generateRunConfig.set(true)
      runDirectory.set(layout.projectDirectory.dir("run/server"))
    }
  }
}

configurations.implementation {
  extendsFrom(configurations.include.get())
}

dependencies {
  minecraft(libs.minecraft.v12111)
  mappings(loom.officialMojangMappings())
  modImplementation(libs.fabric.loader.v12111)
  modImplementation(libs.fabric.api.v12111)

  include(project(":packages:common"))
  include(libs.bytebuddy.core)
  // Loom includes are non-transitive, so GraalJS runtime dependencies must be nested explicitly.
  include(libs.graal.collections)
  include(libs.graal.nativeimage)
  include(libs.graal.word)
  include(libs.graal.jniutils)
  include(libs.graal.polyglot)
  include(libs.graal.truffle.api)
  include(libs.graal.js.language)
  include(libs.graal.regex)
  include(libs.graal.shadowed.icu4j)
  include(libs.graal.shadowed.xz)
}

tasks.processResources {
  val version = project.version.toString()
  val properties =
      mapOf(
          "version" to version,
          "minecraftVersion" to targetMinecraftVersion,
          "loaderVersion" to loaderVersion,
          "fabricApiVersion" to fabricApiVersion,
      )
  inputs.properties(properties)

  filesMatching("fabric.mod.json") {
    expand(properties)
  }
}

java {
  withSourcesJar()
  toolchain.languageVersion.set(javaVersion)
}

tasks.jar {
  val projectName = rootProject.name
  inputs.property("projectName", projectName)

  from(rootProject.file("LICENSE")) {
    rename { "${it}_$projectName" }
  }
}

publishing {
  publications {
    register<MavenPublication>("mavenJava") {
      from(components["java"])
    }
  }
}
