plugins {
    id("java")
}

group = "dev.ftcplus"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":core"))
    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

tasks.register("generateCatalogJson") {
    group = "ftcplus"
    description = "Generates catalog.json for the CLI"

    val outputFile = layout.buildDirectory.file("catalog.json")
    outputs.file(outputFile)

    doLast {
        val motors = listOf(
            mapOf("id" to "gobilda-yellowjacket-6000rpm",  "name" to "goBILDA Yellow Jacket 6000 RPM",  "brand" to "goBILDA", "series" to "Yellow Jacket", "constant" to "GoBILDA.YellowJacket.W5203_6000RPM",  "tpr" to 28.0,    "freeSpeedRpm" to 6000.0, "stallTorqueNm" to 0.144),
            mapOf("id" to "gobilda-yellowjacket-1620rpm",  "name" to "goBILDA Yellow Jacket 1620 RPM",  "brand" to "goBILDA", "series" to "Yellow Jacket", "constant" to "GoBILDA.YellowJacket.W5203_1620RPM",  "tpr" to 103.6,   "freeSpeedRpm" to 1620.0, "stallTorqueNm" to 0.530),
            mapOf("id" to "gobilda-yellowjacket-1150rpm",  "name" to "goBILDA Yellow Jacket 1150 RPM",  "brand" to "goBILDA", "series" to "Yellow Jacket", "constant" to "GoBILDA.YellowJacket.W5203_1150RPM",  "tpr" to 145.6,   "freeSpeedRpm" to 1150.0, "stallTorqueNm" to 0.775),
            mapOf("id" to "gobilda-yellowjacket-435rpm",   "name" to "goBILDA Yellow Jacket 435 RPM",   "brand" to "goBILDA", "series" to "Yellow Jacket", "constant" to "GoBILDA.YellowJacket.W5203_435RPM",   "tpr" to 384.5,   "freeSpeedRpm" to 435.0,  "stallTorqueNm" to 1.834),
            mapOf("id" to "gobilda-yellowjacket-312rpm",   "name" to "goBILDA Yellow Jacket 312 RPM",   "brand" to "goBILDA", "series" to "Yellow Jacket", "constant" to "GoBILDA.YellowJacket.W5203_312RPM",   "tpr" to 537.6,   "freeSpeedRpm" to 312.0,  "stallTorqueNm" to 2.383),
            mapOf("id" to "gobilda-yellowjacket-223rpm",   "name" to "goBILDA Yellow Jacket 223 RPM",   "brand" to "goBILDA", "series" to "Yellow Jacket", "constant" to "GoBILDA.YellowJacket.W5203_223RPM",   "tpr" to 753.2,   "freeSpeedRpm" to 223.0,  "stallTorqueNm" to 3.727),
            mapOf("id" to "gobilda-yellowjacket-117rpm",   "name" to "goBILDA Yellow Jacket 117 RPM",   "brand" to "goBILDA", "series" to "Yellow Jacket", "constant" to "GoBILDA.YellowJacket.W5203_117RPM",   "tpr" to 1425.2,  "freeSpeedRpm" to 117.0,  "stallTorqueNm" to 6.708),
            mapOf("id" to "gobilda-yellowjacket-84rpm",    "name" to "goBILDA Yellow Jacket 84 RPM",    "brand" to "goBILDA", "series" to "Yellow Jacket", "constant" to "GoBILDA.YellowJacket.W5203_84RPM",    "tpr" to 1993.6,  "freeSpeedRpm" to 84.0,   "stallTorqueNm" to 9.178),
            mapOf("id" to "gobilda-yellowjacket-60rpm",    "name" to "goBILDA Yellow Jacket 60 RPM",    "brand" to "goBILDA", "series" to "Yellow Jacket", "constant" to "GoBILDA.YellowJacket.W5203_60RPM",    "tpr" to 2786.0,  "freeSpeedRpm" to 60.0,   "stallTorqueNm" to 13.062),
            mapOf("id" to "gobilda-yellowjacket-43rpm",    "name" to "goBILDA Yellow Jacket 43 RPM",    "brand" to "goBILDA", "series" to "Yellow Jacket", "constant" to "GoBILDA.YellowJacket.W5203_43RPM",    "tpr" to 3892.0,  "freeSpeedRpm" to 43.0,   "stallTorqueNm" to 18.148),
            mapOf("id" to "gobilda-yellowjacket-30rpm",    "name" to "goBILDA Yellow Jacket 30 RPM",    "brand" to "goBILDA", "series" to "Yellow Jacket", "constant" to "GoBILDA.YellowJacket.W5203_30RPM",    "tpr" to 5264.0,  "freeSpeedRpm" to 30.0,   "stallTorqueNm" to 24.517)
        )

        val servos = listOf(
            mapOf("id" to "gobilda-dual-mode-torque",       "name" to "goBILDA Dual Mode Torque",        "brand" to "goBILDA", "series" to "Dual Mode", "constant" to "GoBILDA.Servo.DUAL_MODE_TORQUE",        "travelDegrees" to 300.0, "stallTorqueNm" to 2.118),
            mapOf("id" to "gobilda-dual-mode-speed",        "name" to "goBILDA Dual Mode Speed",         "brand" to "goBILDA", "series" to "Dual Mode", "constant" to "GoBILDA.Servo.DUAL_MODE_SPEED",         "travelDegrees" to 300.0, "stallTorqueNm" to 0.912),
            mapOf("id" to "gobilda-dual-mode-super-speed",  "name" to "goBILDA Dual Mode Super Speed",   "brand" to "goBILDA", "series" to "Dual Mode", "constant" to "GoBILDA.Servo.DUAL_MODE_SUPER_SPEED",   "travelDegrees" to 300.0, "stallTorqueNm" to 0.461),
            mapOf("id" to "gobilda-5turn-torque",           "name" to "goBILDA 5-Turn Torque",           "brand" to "goBILDA", "series" to "Dual Mode", "constant" to "GoBILDA.Servo.DUAL_MODE_5TURN_TORQUE",  "travelDegrees" to 1800.0,"stallTorqueNm" to 2.118),
            mapOf("id" to "gobilda-5turn-speed",            "name" to "goBILDA 5-Turn Speed",            "brand" to "goBILDA", "series" to "Dual Mode", "constant" to "GoBILDA.Servo.DUAL_MODE_5TURN_SPEED",   "travelDegrees" to 1800.0,"stallTorqueNm" to 0.912),
            mapOf("id" to "gobilda-5turn-super-speed",      "name" to "goBILDA 5-Turn Super Speed",      "brand" to "goBILDA", "series" to "Dual Mode", "constant" to "GoBILDA.Servo.DUAL_MODE_5TURN_SUPER_SPEED","travelDegrees" to 1800.0,"stallTorqueNm" to 0.461),
            mapOf("id" to "gobilda-proton-torque",          "name" to "goBILDA Proton Torque",           "brand" to "goBILDA", "series" to "Proton",    "constant" to "GoBILDA.Servo.PROTON_TORQUE",           "travelDegrees" to 180.0, "stallTorqueNm" to 1.324),
            mapOf("id" to "gobilda-proton-speed",           "name" to "goBILDA Proton Speed",            "brand" to "goBILDA", "series" to "Proton",    "constant" to "GoBILDA.Servo.PROTON_SPEED",            "travelDegrees" to 180.0, "stallTorqueNm" to 0.589)
        )

        val catalog = mapOf("motors" to motors, "servos" to servos, "crservos" to emptyList<Any>())

        val gson = groovy.json.JsonOutput.toJson(catalog)
        outputFile.get().asFile.writeText(groovy.json.JsonOutput.prettyPrint(gson))
        println("Generated catalog.json")
    }
}