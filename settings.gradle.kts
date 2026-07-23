pluginManagement {
  includeBuild("build-logic")

  repositories {
    maven {
      name = "Fabric"
      url = uri("https://maven.fabricmc.net/")
    }
    mavenCentral()
    gradlePluginPortal()
  }
}

// Should match your modid
rootProject.name = "myqolpackages"

include("packages:common")

include("packages:fabric-1.21.11")
