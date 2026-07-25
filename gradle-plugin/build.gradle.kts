plugins {
    `java-gradle-plugin`
    `maven-publish`
}

group = "dev.ftcplus"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.ow2.asm:asm:9.7")
    implementation("org.ow2.asm:asm-commons:9.7")
}

gradlePlugin {
    plugins {
        create("ftcplus") {
            id = "dev.ftcplus"
            implementationClass = "dev.ftcplus.gradle.FtcPlusPlugin"
        }
    }
}