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
    implementation("com.mergen:mergen-sdk:2.3.0")
    // Опционально, только для QA-сборок (PII-диагностика):
    // debugImplementation("com.mergen:mergen-sdk-debugger:2.3.0")
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

Готовый sample-проект — [`sample/`](sample/): один экран с полным флоу
(скан обеих сторон, гейт стороны/поколения, verify, результат, ошибки),
SDK подтягивается из GitHub Packages этого репозитория. Откройте папку в
Android Studio, положите ваш `license.json` в `app/src/main/assets/` — и
запускайте.

## Документация

Полный комплект на русском — гайды по интеграции, справочники API обеих
платформ (Dokka для Kotlin, DocC для Swift) и готовые примеры-файлы:

**[mergen-docs-2.3.0.zip](https://github.com/DanielPHP01/MergenSDK-iOS/releases/download/v2.3.0/mergen-docs-2.3.0.zip)** (18 МБ)

Распакуйте и откройте `site/guide/index.html`; справочник Android —
`site/api/android/index.html` (работает прямо из файла).

## Версии

Семантическое версионирование: breaking changes только в мажорных версиях.
Всё, что помечено `@Deprecated` в 2.3.0, будет удалено в 3.0 — сообщение
каждой аннотации называет замену.
