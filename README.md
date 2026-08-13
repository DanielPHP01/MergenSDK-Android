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
    implementation("com.mergen:mergen-sdk:2.6.0")
    // Опционально, только для QA-сборок (PII-диагностика):
    // debugImplementation("com.mergen:mergen-sdk-debugger:2.6.0")
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

## Что нового в 2.6.0

Кадр-гейт `m16` — отдельная модель (2.8 МБ, вход 256×160 — выпрямленный кроп
карты), которую SDK опрашивает каждые 350 мс, пока пользователь наводит камеру.

* **Авто-разворот карты.** Перевёрнутая карта разворачивается ДО распознавания
  (точность 0.9963 на реальных кропах). Это убирает повторный прогон каскада на
  180° — секунды на устройстве — и лечит ошибку определения стороны, когда карту
  поднесли вверх ногами.
* **Палец на данных блокирует захват.** Рамка краснеет, подсказка называет
  закрытое поле («Уберите палец с номера документа»); верное поле называется в
  8 случаях из 10. Порог 0.99: пойманы 11 кадров с пальцем из 12 реальных,
  ложных срабатываний нет — 0 из 68 чистых кропов, 0 из 12 «карту держат за
  кромку», 0 из 38 фронтов с портретом (портрет владельца гейт не путает с рукой).
* **Предохранитель.** Непрерывная блокировка дольше 20 с снимается: ложное
  срабатывание не может сделать документ несканируемым.
* Головы `shadow` и `dirt` **выключены по умолчанию**. Тень ложно срабатывала на
  живой съёмке — телефон, поднесённый к карте, сам отбрасывает на неё тень;
  у грязи recall 0.300 на честных пятнах. Обе включаются конфигом без пересборки.

Ассет модели добавляет к AAR ≈2.8 МБ.

### Ломающее изменение: `ScanStatus.Occluded`

В `ScanStatus` появилась ветка `Occluded` — исчерпывающий `when` без неё
перестанет компилироваться. Раньше на таких кадрах приходил `HoldSteady`
(«держите неподвижно»), что противоречило красной рамке.

```kotlin
val text = when (val s = state.scanStatus) {
    ScanStatus.NoCard      -> "Поднесите карту"
    ScanStatus.TooFar      -> "Поднесите ближе"
    // ← новая ветка 2.6.0
    ScanStatus.Occluded    -> when (state.occlusionType) {
        OcclusionType.FINGER -> "Уберите палец с карты"
        OcclusionType.SHADOW -> "Уберите тень"
        OcclusionType.NONE   -> ""
    }
    is ScanStatus.Finished -> if (s.success) "" else "Не удалось"
    else                   -> ""
}
```

Своя ветка нужна только тем, кто рисует собственные тексты: готовая строка SDK
(`state.messageTitle`) уже содержит формулировку с именем закрытого поля.

### Новые поля состояния кадра

`ScannerFrameState` — то, что получает оверлей и drop-in UI:

| Поле | Что означает |
|---|---|
| `isFingerOccluded` | палец / рука лежит на карте |
| `isShadowed` | жёсткая локальная тень (голова выключена по умолчанию) |
| `occludedFieldId` | какое поле закрыто: `ocr::FieldId` как int, `100` = фото владельца, `-1` = поле не названо |
| `occlusionType` | `OcclusionType.NONE` / `FINGER` / `SHADOW` |

Те же данные в headless-режиме — в `FrameResult`: `isFingerOccluded`,
`isShadowed`, `occludedFieldId` и `occlusionChannel` (`0` = палец, `2` = тень,
`-1` = ничего).

### Настройка гейта

Пороги живут в движке: `MergenScannerConfig` новых полей не получил, ключи
задаются в `assets/config.json` вашего приложения.

| Ключ | По умолчанию | Что делает |
|---|---|---|
| `enable_quality_gate` | `true` | кадр-гейт целиком |
| `quality_gate_finger_threshold` | `0.99` | порог пальца |
| `quality_gate_shadow_threshold` | `1.1` | тень; `1.1` = никогда, включить = поставить ≤ 1.0 |
| `quality_gate_dirt_threshold` | `1.1` | грязь, выключена так же |
| `quality_gate_block_timeout_ms` | `20000` | предохранитель непрерывной блокировки |
| `quality_gate_probe_interval_ms` | `350` | как часто опрашивается модель при наведении |
| `quality_gate_field_coverage` | `0.25` | какую долю поля должна закрыть помеха, чтобы поле назвали |
| `quality_gate_auto_rotate` | `true` | авто-разворот перевёрнутой карты |

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

Исключение — `ScanStatus.Occluded` в 2.6.0: новая ветка sealed-интерфейса ломает
только исчерпывающий `when` при компиляции. Ничего не удалено и не переименовано,
контракт `onFinished` тот же, бинарная совместимость сохранена — меняется
поведение сканера (кадр с пальцем теперь не захватывается), не сигнатуры.
