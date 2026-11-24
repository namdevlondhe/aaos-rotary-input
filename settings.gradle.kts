pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Only for the sample's dependency on aaos-hmi-components.
        maven("https://jitpack.io") {
            content { includeGroup("com.github.namdevlondhe.aaos-hmi-components") }
        }
    }
}

rootProject.name = "aaos-rotary-input"

include(":input-core")
include(":rotary-compose")
include(":sample")
