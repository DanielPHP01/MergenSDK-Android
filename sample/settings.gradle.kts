// settings.gradle.kts — Mergen SDK Sample (Maven consumer)
// Этот проект получает SDK исключительно из GitHub Packages (com.mergen:mergen-sdk:2.3.0).
// Никаких ссылок на локальные модули или AAR-файлы.

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    // FAIL_ON_PROJECT_REPOS: любая попытка объявить repositories в build.gradle
    // модуля вызовет ошибку сборки. Гарантирует, что все зависимости
    // резолвятся только из репозиториев, объявленных здесь.
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    repositories {
        google()
        mavenCentral()

        // ── Mergen SDK (GitHub Packages) ──────────────────────────────────────
        // Требует gpr.user / gpr.key в ~/.gradle/gradle.properties.
        // Токен должен иметь право read:packages.
        maven {
            url = uri("https://maven.pkg.github.com/DanielPHP01/MergenSDK-Android")
            credentials {
                username = providers.gradleProperty("gpr.user").orNull
                password = providers.gradleProperty("gpr.key").orNull
            }
        }
    }
}

rootProject.name = "MergenSample-Android"
include(":app")
