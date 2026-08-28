plugins {
  `java-library`
  `maven-publish`
}

val javaVersion = JavaLanguageVersion.of(21)

val compileOnlyAndTestRuntime =
    configurations.create("compileOnlyAndTestRuntime") {
      isCanBeConsumed = false
      isCanBeResolved = false
    }

configurations {
  compileOnly { extendsFrom(compileOnlyAndTestRuntime) }
  testRuntimeOnly { extendsFrom(compileOnlyAndTestRuntime) }
}

dependencies {
  // GraalVM
  implementation(libs.graal.polyglot.get())
  implementation(libs.graal.js.core) {
    exclude(group = "org.graalvm.truffle", module = "truffle-runtime")
  }
  // ByteBuddy
  implementation(libs.bytebuddy.core.get())

  // GSON & SLF4J provided at runtime
  compileOnlyAndTestRuntime(libs.gson)
  compileOnlyAndTestRuntime(libs.jetbrains.annotations)
  compileOnlyAndTestRuntime(libs.slf4j.api)
}

java {
  withSourcesJar()
  toolchain.languageVersion.set(javaVersion)
}

publishing {
  publications {
    register<MavenPublication>("mavenJava") {
      from(components["java"])
    }
  }
}
