import io.github.trethore.buildlogic.unpack

plugins {
  `java-library`
  `maven-publish`
}

dependencies {
  val graalVersion = providers.gradleProperty("graal_version").get()
  val byteBuddyVersion = providers.gradleProperty("byte_buddy_version").get()

  // GraalVM dependencies
  unpack(implementation("org.graalvm.sdk:graal-sdk:$graalVersion"))
  unpack(implementation("org.graalvm.truffle:truffle-api:$graalVersion"))
  implementation("org.graalvm.js:js:$graalVersion")
  unpack(create("org.graalvm.js:js-language:$graalVersion"))
  unpack(implementation("org.graalvm.js:js-scriptengine:$graalVersion"))
  // ByteBuddy dependencies
  unpack(implementation("net.bytebuddy:byte-buddy:$byteBuddyVersion"))
  unpack(implementation("net.bytebuddy:byte-buddy-agent:$byteBuddyVersion"))

  implementation("com.google.code.gson:gson:${providers.gradleProperty("gson_version").get()}")
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
