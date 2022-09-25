import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.10"
    jacoco
    id("com.vanniktech.maven.publish") version "0.22.0"
}

val jacksonVersion = "2.13.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    api("com.fasterxml.jackson.core:jackson-core:$jacksonVersion")
    api("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

// pass -PreadmeFormat to format benchmark results to update readme
val readmeFormat = hasProperty("readmeFormat")

val kotestVersion = "4.6.4"

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.6.2")
    testApi("com.jayway.jsonpath:json-path:2.4.0")
    testImplementation("org.json:json:20180813")
    testImplementation("io.kotest:kotest-runner-junit5-jvm:$kotestVersion") // for kotest framework
    testImplementation("io.kotest:kotest-assertions-core-jvm:$kotestVersion") // for kotest core jvm assertions
}

tasks.register<Test>("benchmark") {
    useJUnitPlatform()

    if(readmeFormat) {
        jvmArgs("-DreadmeFormat=true")
    }

    filter {
        include("com/nfeld/jsonpathkt/BenchmarkTest.class")
    }

    testLogging {
        showStandardStreams = true
    }

    // Make this task never up to date, thus forcing rerun of all tests whenever task is run
    outputs.upToDateWhen { false }
}

tasks.register<Test>("perfTest") {
    useJUnitPlatform()

    filter {
        include("com/nfeld/jsonpathkt/PerfTest.class")
    }

    testLogging {
        // show test results for following events
        events("PASSED", "FAILED", "SKIPPED")

        // show printlines
        showStandardStreams = true
    }

    // Make this task never up to date, thus forcing rerun of all tests whenever task is run
    outputs.upToDateWhen { false }
}

tasks.test {
    useJUnitPlatform()

    filter {
        exclude(
            "com/nfeld/jsonpathkt/BenchmarkTest.class",
            "com/nfeld/jsonpathkt/PerfTest.class"
        )
    }

    testLogging {
        // show test results for following events
        events("PASSED", "FAILED", "SKIPPED")

        // show printlines
        showStandardStreams = true
    }

    addTestListener(object : TestListener {
        override fun beforeSuite(suite: TestDescriptor?) {}
        override fun afterTest(testDescriptor: TestDescriptor?, result: TestResult?) {}
        override fun beforeTest(testDescriptor: TestDescriptor?) {}

        override fun afterSuite(suite: TestDescriptor?, result: TestResult?) {
            val parent = suite?.parent
            if(parent != null && result != null) {
                println("\nTest result: ${result.resultType}")
                println(
                    """
                    |Test summary: ${result.testCount} tests,
                    | ${result.successfulTestCount} succeeded,
                    | ${result.failedTestCount} failed,
                    | ${result.skippedTestCount} skipped
                    """.trimMargin().replace("\n", "")
                )
           }
        }

    })

    configure<JacocoTaskExtension> {
        setDestinationFile(layout.buildDirectory.file("jacoco/junitPlatformTest.exec").map { it.asFile })
        isIncludeNoLocationClasses = true
        excludes = listOf(
            "*/LRUCache\$LRUMap*",
            "*/JsonNodeKt*",
            "jdk.internal.*"
        )
    }

    // Make this task never up to date, thus forcing rerun of all tests whenever task is run
    outputs.upToDateWhen { false }
}

jacoco {
    toolVersion = "0.8.8"
    reportsDirectory.set(layout.buildDirectory.dir("reports"))
}

tasks.jacocoTestReport.configure {
    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }
}

tasks.jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = BigDecimal(0.90)
            }
        }
    }
}

tasks.check.configure {
    dependsOn(tasks.jacocoTestCoverageVerification)
}
