import net.fabricmc.loom.api.LoomGradleExtensionAPI
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.testing.Test

val modVersion = libs.versions.mod.get()

plugins {
  alias(libs.plugins.fabric.loom.remap) apply false
  `maven-publish`
  id("com.diffplug.spotless") version "8.8.0"
  id("example.unpack-sources")
  id("example.sonar")
}

spotless {
  java {
    target("**/src/**/*.java")
    targetExclude("references/**", "**/build/**")
    licenseHeaderFile(rootProject.file("HEADER"))
    importOrder()
    removeUnusedImports()
    palantirJavaFormat(libs.versions.palantir.java.format.get())
    trimTrailingWhitespace()
    endWithNewline()
    toggleOffOn()
  }

  format("javaPackageInfo") {
    target("**/src/**/package-info.java")
    targetExclude("references/**", "**/build/**")
    licenseHeaderFile(rootProject.file("HEADER"), "(?=/\\*\\*)")
  }

  kotlinGradle {
    target("**/*.gradle.kts")
    targetExclude("references/**", "**/build/**", ".gradle/**")
    ktfmt()
  }

  format("javascript") {
    target("**/*.js")

    trimTrailingWhitespace()
    endWithNewline()
  }

  format("misc") {
    target("**/*.md", ".gitignore")
    targetExclude("references/**", "**/build/**", ".gradle/**")
    trimTrailingWhitespace()
    leadingTabsToSpaces()
    endWithNewline()
  }
}

allprojects {
  version = modVersion
  group = providers.gradleProperty("maven_group").get()

  repositories {
    mavenCentral()
    maven {
      name = "Fabric"
      url = uri("https://maven.fabricmc.net/")
    }
    maven {
      name = "Mojang"
      url = uri("https://libraries.minecraft.net/")
    }
  }
}

subprojects {
  pluginManager.withPlugin("net.fabricmc.fabric-loom-remap") {
    extensions.configure<LoomGradleExtensionAPI> {
      runs.configureEach {
        preferGradleTask.set(true)
      }
    }
  }

  plugins.withType<JavaPlugin> {
    dependencies {
      "testImplementation"(libs.junit.jupiter)
      "testRuntimeOnly"("org.junit.platform:junit-platform-launcher")
    }

    tasks.withType<Test>().configureEach {
      useJUnitPlatform()
    }
  }
}

references {
  unpackNestedJars = true

  // Optional Git references can be added like this:
  // git(
  //     url = "https://github.com/FabricMC/fabric.git",
  //     branch = "main",
  //     commit = null,
  // )
}
