import io.github.trethore.buildlogic.unpack

plugins {
  `java-library`
  `maven-publish`
}

configurations.testRuntimeOnly {
  extendsFrom(configurations.compileOnly.get())
}

dependencies {
  // GraalVM
  unpack(implementation(libs.graal.sdk.get()))
  unpack(implementation(libs.graal.truffle.api.get()))
  implementation(libs.graal.js.core)
  unpack(implementation(libs.graal.js.scriptengine.get()))
  // ByteBuddy
  unpack(implementation(libs.bytebuddy.core.get()))
  unpack(implementation(libs.bytebuddy.agent.get()))

  // Source
  unpack(create(libs.graal.js.language.get()))

  // GSON & SLF4J provided at runtime
  compileOnly(libs.gson)
  compileOnly(libs.slf4j.api)
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
