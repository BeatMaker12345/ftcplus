plugins {
    `java`
    application
}

group = "dev.ftcplus"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.github.javaparser:javaparser-core:3.25.10")
    implementation("com.google.code.gson:gson:2.10.1")
}

application {
    mainClass.set("dev.ftcplus.ast.AstTool")
}

tasks.jar {
    archiveFileName.set("ftcplus-ast.jar")
    manifest {
        attributes["Main-Class"] = "dev.ftcplus.ast.AstTool"
    }
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}