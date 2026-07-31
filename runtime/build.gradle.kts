plugins {
    `java-library`
    `maven-publish`
}

group = "dev.ftcplus"
version = "1.0-SNAPSHOT"

repositories { mavenCentral() }

dependencies {
    implementation(project(":core"))
}

java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(17)) }
}

publishing {
    publications {
        create<MavenPublication>("maven") { from(components["java"]) }
    }
}