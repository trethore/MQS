plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation(gradleApi())
    implementation("com.github.spotbugs:com.github.spotbugs.gradle.plugin:6.4.8")
    implementation("net.ltgt.errorprone:net.ltgt.errorprone.gradle.plugin:5.1.0")
    implementation("org.danilopianini.cpd:org.danilopianini.cpd.gradle.plugin:4.0.14")
}
