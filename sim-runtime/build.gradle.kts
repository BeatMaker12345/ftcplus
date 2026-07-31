plugins {
    `java-library`
    application
    `maven-publish`
}

group = "dev.ftcplus"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven { url = uri("https://mymaven.bylazar.com/releases") }
}

dependencies {
    implementation(project(":core"))
    implementation(project(":runtime"))
    implementation(project(":drivetrains"))
    implementation(project(":catalog"))
    implementation(project(":dashboard"))

    // gamepad support
    implementation("net.java.jinput:jinput:2.0.9")
    implementation("net.java.jinput:jinput:2.0.9:natives-all")
}

application {
    mainClass.set("dev.ftcplus.sim.SimLauncher")
}

java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(17)) }
}

publishing {
    publications {
        create<MavenPublication>("maven") { from(components["java"]) }
    }
}
