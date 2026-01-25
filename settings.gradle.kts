pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
        // Correct the URL on the line below
        maven { url = uri("https://maven.maplibre.org/repository/maven-public/") }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Also correct the URL on the line below
        maven { url = uri("https://maven.maplibre.org/repository/maven-public/") }
    }
}

rootProject.name = "Beacon"
include(":app")