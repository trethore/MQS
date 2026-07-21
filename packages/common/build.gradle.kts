import io.github.trethore.buildlogic.unpack

plugins {
  `java-library`
  `maven-publish`
}

configurations.testRuntimeOnly {
  extendsFrom(configurations.compileOnly.get())
}

dependencies {
  val graalVersion = providers.gradleProperty("graal_version").get()
  val byteBuddyVersion = providers.gradleProperty("byte_buddy_version").get()
  val gsonVersion = providers.gradleProperty("gson_version").get()
  val slf4jApiVersion = providers.gradleProperty("slf4j_api_version").get()

  // GraalVM
  unpack(implementation("org.graalvm.sdk:graal-sdk:$graalVersion"))
  unpack(implementation("org.graalvm.truffle:truffle-api:$graalVersion"))
  implementation("org.graalvm.js:js:$graalVersion")
  unpack(implementation("org.graalvm.js:js-scriptengine:$graalVersion"))
  // ByteBuddy
  unpack(implementation("net.bytebuddy:byte-buddy:$byteBuddyVersion"))
  unpack(implementation("net.bytebuddy:byte-buddy-agent:$byteBuddyVersion"))

  // Source
  unpack(create("org.graalvm.js:js-language:$graalVersion"))

  // GSON & SLF4J provided at runtime
  compileOnly("com.google.code.gson:gson:$gsonVersion")
  compileOnly("org.slf4j:slf4j-api:$slf4jApiVersion")
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
