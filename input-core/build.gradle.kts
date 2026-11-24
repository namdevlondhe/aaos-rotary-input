import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Pure Kotlin: gesture timing, rotary acceleration and area geometry are tested on the plain JVM.
plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-library`
    `maven-publish`
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
    withSourcesJar()
}

kotlin {
    compilerOptions.jvmTarget.set(JvmTarget.JVM_17)
}

dependencies {
    testImplementation(libs.junit)
}

publishing {
    publications {
        create<MavenPublication>("release") {
            from(components["java"])
            groupId = "com.github.namdevlondhe.aaos-rotary-input"
            artifactId = "input-core"
            version = System.getenv("VERSION") ?: "0.1.0"
        }
    }
}
