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
  unpack(implementation(libs.graal.polyglot.get()))
  implementation(libs.graal.js.core) {
    exclude(group = "org.graalvm.truffle", module = "truffle-runtime")
  }
  // ByteBuddy
  unpack(implementation(libs.bytebuddy.core.get()))

  // Source
  unpack(create(libs.graal.js.language.get()))
  unpack(create(libs.graal.regex.get()))
  unpack(create(libs.graal.collections.get()))
  unpack(create(libs.graal.nativeimage.get()))
  unpack(create(libs.graal.word.get()))
  unpack(create(libs.graal.jniutils.get()))

  // GSON & SLF4J provided at runtime
  compileOnly(libs.gson)
  compileOnly(libs.jetbrains.annotations)
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
