plugins {
  `java-library`
  `maven-publish`
}

dependencies {
  implementation("org.slf4j:slf4j-api:${providers.gradleProperty("common_slf4j_version").get()}")
}

tasks.withType<JavaCompile>().configureEach {
  options.release = 21
}

java {
  withSourcesJar()

  sourceCompatibility = JavaVersion.VERSION_21
  targetCompatibility = JavaVersion.VERSION_21
}

publishing {
  publications {
    register<MavenPublication>("mavenJava") {
      from(components["java"])
    }
  }
}
