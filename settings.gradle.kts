pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    // PREFER_SETTINGS: Capacitor Android library declares its own repositories block.
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "My Application"
include(":app")

// Capacitor core Android library from frontend node_modules (no second android/ tree).
include(":capacitor-android")
project(":capacitor-android").projectDir =
    file("frontend/node_modules/@capacitor/android/capacitor")
 