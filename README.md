# Mergen Android SDK

Дистрибуция Mergen SDK для сканирования ID-карт — артефакты публикуются в
GitHub Packages этого репозитория.

## Подключение

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        google(); mavenCentral()
        maven {
            url = uri("https://maven.pkg.github.com/DanielPHP01/MergenSDK-Android")
            credentials {
                username = providers.gradleProperty("gpr.user").orNull
                password = providers.gradleProperty("gpr.key").orNull   // токен с read:packages
            }
        }
    }
}
```

```kotlin
// build.gradle.kts модуля приложения
dependencies {
    implementation("com.mergen:mergen-sdk:2.5.0")
    // Опционально, только для QA-сборок (PII-диагностика):
    // debugImplementation("com.mergen:mergen-sdk-debugger:2.5.0")
}
```

## Быстрый старт

```kotlin
// Верификация двух сторон одной карты:
val result = Mergen.verify(context, front = frontInput, back = backInput, license = licenseJson)

// Готовый UI камеры:
//   Compose — MergenScanView(license = ..., onResult = ...)
//   XML     — com.mergen.sdk.MergenScannerView
//   3 строки — MergenScannerActivity
```

## Пример

Готовый sample-проект — **[MergenSample-Android](https://github.com/DanielPHP01/MergenSample-Android)**:
один экран с полным флоу (скан обеих сторон, гейт стороны/поколения, verify,
результат, ошибки), SDK подтягивается из GitHub Packages этого репозитория,
тестовая лицензия на 7 дней вложена — клонируйте, откройте в Android Studio
и запускайте.

## Документация

Полный комплект на русском — гайды по интеграции, справочники API обеих
платформ (Dokka для Kotlin, DocC для Swift) и готовые примеры-файлы:

**[mergen-docs-2.3.1.zip](https://github.com/DanielPHP01/MergenSDK-iOS/releases/download/v2.3.1/mergen-docs-2.3.1.zip)** (17 МБ)

Начните с раздела **«API на практике»** — там ровно та часть SDK,
которую вызывают sample-проекты.

Распакуйте и откройте `site/guide/index.html`; справочник Android —
`site/api/android/index.html` (работает прямо из файла).

## Версии

Семантическое версионирование: breaking changes только в мажорных версиях.
Всё, что помечено `@Deprecated` в 2.3.0, будет удалено в 3.0 — сообщение
каждой аннотации называет замену.
