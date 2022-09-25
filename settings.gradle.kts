plugins {
    id("com.gradle.enterprise") version "3.10.3"
}

rootProject.name = "jsonpathkt"

val publishBuildScan = providers.gradleProperty("publishBuildScan")

gradleEnterprise {
    buildScan {
        termsOfServiceUrl = "https://gradle.com/terms-of-service"
        termsOfServiceAgree = "yes"

        if (publishBuildScan.isPresent) {
            publishAlways()
        }
    }
}
