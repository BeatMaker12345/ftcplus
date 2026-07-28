plugins {
    `java-library`
    `maven-publish`
}

group = "dev.ftcplus"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    google()
}

dependencies {
    implementation(project(":ftc-runtime"))
    implementation(project(":drivetrains"))
    implementation(project(":core"))
    implementation("org.jetbrains:annotations:24.0.1")
    implementation(project(":limelight"))
    compileOnly(files("../ftc-runtime/libs/RobotCore-classes.jar"))
    compileOnly(files("../ftc-runtime/libs/Hardware-classes.jar"))
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}