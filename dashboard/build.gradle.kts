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
    implementation(project(":core"))
    implementation(project(":limelight"))
    compileOnly(files("../ftc-runtime/libs/RobotCore-classes.jar"))
    compileOnly(files("../ftc-runtime/libs/Hardware-classes.jar"))
    compileOnly(files("../ftc-runtime/libs/android-classes.jar"))
    implementation("org.nanohttpd:nanohttpd:2.3.1")
    implementation("org.nanohttpd:nanohttpd-websocket:2.3.1")
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