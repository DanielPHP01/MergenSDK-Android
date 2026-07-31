# Mergen SDK — Android Sample

Минимальный пример интеграции **Mergen ID Card Scanner SDK v2.3.0** в Android-приложение.
SDK получается исключительно из GitHub Packages (Maven), без локальных AAR-файлов.

## Предварительные требования

| Инструмент | Версия |
|---|---|
| Android Studio | Hedgehog или новее |
| JDK | Corretto 17 (JBR 21 не совместим с AGP 8.x) |
| Android SDK | Platform 36, Build Tools 36.0.0 |
| GitHub токен | Personal Access Token с правом `read:packages` |

## Настройка Maven-доступа

SDK расположен в приватном репозитории GitHub Packages и требует аутентификации.

1. Создайте Personal Access Token (PAT) на GitHub:
   - Перейдите: **GitHub → Settings → Developer settings → Personal access tokens → Tokens (classic)**
   - Выберите право: `read:packages`

2. Добавьте токен в `~/.gradle/gradle.properties` (файл вне репозитория — токен не попадёт в git):

```properties
gpr.user=ВАШ_GITHUB_USERNAME
gpr.key=ВАШ_GITHUB_PAT_ТОКЕН
```

## Установка license.json

SDK **не инициализируется** без валидного лицензионного файла. Получите `license.json` от команды Mergen (sdk@mergen.kz).

Положите файл по пути:
```
app/src/main/assets/license.json
```

Убедитесь, что поле `app_id` в лицензии совпадает с `applicationId` приложения:
```
com.mergen.sample
```

Если `license.json` отсутствует, приложение покажет экран с инструкциями вместо краша.

## Запуск

### Android Studio

1. Откройте папку `samples/MergenSample-Android/` как отдельный проект в Android Studio.
2. При первом открытии Studio предложит выбрать JDK — выберите **Corretto 17**.
3. Дождитесь синхронизации Gradle (SDK будет скачан из GitHub Packages, ~86 МБ).
4. Запустите на устройстве или эмуляторе с камерой.

### Командная строка

```bash
cd samples/MergenSample-Android
./gradlew :app:assembleDebug
```

## Структура проекта

```
MergenSample-Android/
├── app/
│   └── src/main/
│       ├── assets/           ← сюда кладётся license.json
│       └── java/com/mergen/sample/
│           ├── MainActivity.kt          — точка входа, проверка лицензии
│           └── ui/
│               ├── QuickstartScreen.kt  — полный флоу сканирования (из docs/examples)
│               └── NoLicenseScreen.kt   — экран-инструкция при отсутствии лицензии
├── settings.gradle.kts      ← GitHub Packages репозиторий
└── build.gradle.kts
```

## Флоу приложения

```
MainActivity
  ↓ license.json отсутствует → NoLicenseScreen (инструкции)
  ↓ license.json найден      → QuickstartScreen
                                  ↓ ScanFront (MergenScanView, SideGateConfig.frontOnly)
                                  ↓ ScanBack  (MergenScanView, SideGateConfig.back(generation))
                                  ↓ Verifying (Mergen.verify)
                                  ↓ Result / Error
```

## Важные замечания

- **Только `arm64-v8a`**: SDK не поддерживает x86/x86_64. Тестируйте на реальном устройстве.
- **Лицензия и `applicationId`**: они должны совпадать; несоответствие вызывает `MergenException`.
- **AntiDebug**: в релизной сборке SDK блокирует запуск под отладчиком — это штатное поведение.

## Зависимости SDK (транзитивные, не нужно добавлять вручную)

SDK тянет из Maven: CameraX, ML Kit OCR, ONNX Runtime, OpenCV, accompanist-permissions, Compose activity/lifecycle API.
