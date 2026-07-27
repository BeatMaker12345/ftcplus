plugins {
    `java-library`
    `maven-publish`
}

group = "dev.ftcplus"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":core"))
    compileOnly(files("libs/RobotCore-classes.jar"))
    compileOnly(files("libs/Hardware-classes.jar"))
    compileOnly(files("libs/android-classes.jar"))
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