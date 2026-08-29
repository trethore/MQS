import net.fabricmc.loom.api.LoomGradleExtensionAPI
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.testing.Test
import org.jetbrains.qodana.tasks.QodanaScanTask

val modVersion = providers.gradleProperty("mod_version").get()

plugins {
  alias(libs.plugins.fabric.loom.remap) apply false
  alias(libs.plugins.qodana)
  alias(libs.plugins.spotless)
  `maven-publish`
  id("io.github.trethore.sonar")
}

spotless {
  java {
    target("**/src/**/*.java")
    targetExclude("**/build/**")
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
    targetExclude("**/build/**")
    licenseHeaderFile(rootProject.file("HEADER"), "(?=/\\*\\*)")
  }

  kotlinGradle {
    target("**/*.gradle.kts")
    targetExclude("**/build/**", ".gradle/**")
    ktfmt()
  }

  format("javascript") {
    target("**/*.js")

    trimTrailingWhitespace()
    endWithNewline()
  }

  format("misc") {
    target("**/*.md", ".gitignore")
    targetExclude("**/build/**", ".gradle/**")
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

tasks.named<QodanaScanTask>("qodanaScan") {
  arguments.addAll(
      "--config",
      "config/qodana/qodana.yaml",
      "--env",
      "JAVA_TOOL_OPTIONS=-Dorg.gradle.projectcachedir=/data/cache/gradle/project-cache",
      "--print-problems",
      "--disable-update-checks",
  )
}

subprojects {
  pluginManager.withPlugin("net.fabricmc.fabric-loom-remap") {
    extensions.configure<LoomGradleExtensionAPI> {
      runs.configureEach {
        preferGradleTask.set(true)
        systemProperties.put("fabric.log.disableAnsi", "false")
      }
    }
  }

  plugins.withType<JavaPlugin> {
    dependencies {
      "testImplementation"(libs.junit.jupiter)
      "testRuntimeOnly"(libs.junit.platform.launcher)
    }

    tasks.withType<Test>().configureEach {
      useJUnitPlatform()
    }
  }
}
