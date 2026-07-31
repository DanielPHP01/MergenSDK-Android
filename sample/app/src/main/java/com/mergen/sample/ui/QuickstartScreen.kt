//
// QuickstartScreen.kt — полный минимальный флоу Mergen SDK (Jetpack Compose).
//
// Сценарий: скан лицевой стороны → скан обратной стороны → верификация →
// экран результата (или ошибки) → «Сканировать заново».
//
// Источник: docs/examples/android/QuickstartScreen.kt (выверенный API v2.3).
// Изменено: package перенесён в com.mergen.sample.ui для sample-проекта.
//

package com.mergen.sample.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mergen.sdk.MIDSide
import com.mergen.sdk.Mergen
import com.mergen.sdk.MergenException
import com.mergen.sdk.MergenScanResult
import com.mergen.sdk.MergenScanView
import com.mergen.sdk.MergenSideInput
import com.mergen.sdk.ScanSpeed
import com.mergen.sdk.SideGateConfig
import com.mergen.sdk.VerifyResult
import com.mergen.sdk.VerifySideIssue
import com.mergen.sdk.VerifyStatus
import com.mergen.sdk.backIssue
import com.mergen.sdk.frontIssue
import com.mergen.sdk.loadLicense
import kotlinx.coroutines.launch

// ── Состояние флоу ───────────────────────────────────────────────────────────
// Простая машина из пяти шагов. Compose сам пересоздаёт экраны при смене шага.
private sealed interface QuickstartStep {
    object ScanFront : QuickstartStep                       // открыт сканер лицевой стороны
    object ScanBack : QuickstartStep                        // открыт сканер обратной стороны
    object Verifying : QuickstartStep                       // идёт верификация (спиннер)
    data class Result(val result: VerifyResult) : QuickstartStep  // итог верификации
    data class Error(val message: String) : QuickstartStep  // готовый текст ошибки для пользователя
}

@Composable
internal fun QuickstartScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // ── Лицензия ─────────────────────────────────────────────────────────────
    // Mergen.loadLicense читает assets/license.json. Без валидной лицензии
    // движок не инициализируется.
    val license = remember { Mergen.loadLicense(context) ?: "" }

    // Прогрев движка и ML-моделей на старте — первое открытие камеры и первый
    // verify проходят без паузы на инициализацию. Повторные вызовы — no-op.
    // В реальном приложении лучше вызывать один раз в Application.onCreate
    // (см. руководство, раздел 9).
    LaunchedEffect(Unit) { Mergen.preload(context, license) }

    var step by remember { mutableStateOf<QuickstartStep>(QuickstartStep.ScanFront) }

    // Результат скана лицевой стороны храним до конца флоу: он нужен и для
    // verify, и для гейта поколения на обратной стороне.
    var frontScan by remember { mutableStateOf<MergenScanResult?>(null) }

    // ── Верификация ──────────────────────────────────────────────────────────
    // Mergen.verify — единственная точка входа. Для двух камерных сканов SDK
    // сначала выполняет мгновенную сверку уже распознанных данных (~1 мс) и
    // только при неубедительном итоге запускает полный OCR-прогон. Внутри
    // вызов сам уходит на Dispatchers.IO — можно запускать из UI-скоупа.
    fun startVerify(front: MergenScanResult, back: MergenScanResult) {
        step = QuickstartStep.Verifying
        scope.launch {
            step = try {
                val result = Mergen.verify(
                    context = context,
                    front   = MergenSideInput.Scan(front),
                    back    = MergenSideInput.Scan(back),
                    license = license,
                )
                QuickstartStep.Result(result)
            } catch (e: Exception) {
                // MergenException.userMessage — готовая русская строка для показа
                // пользователю (лицензия отсутствует, движок не поднялся и т.д.).
                // Не парсите e.message — это девелоперский текст.
                QuickstartStep.Error(
                    (e as? MergenException)?.userMessage
                        ?: e.localizedMessage
                        ?: "Непредвиденная ошибка"
                )
            }
        }
    }

    when (val current = step) {
        QuickstartStep.ScanFront -> QuickstartScanScreen(
            side    = MIDSide.FRONT,
            license = license,
            // Гейт стороны: сканер физически не примет обратную сторону
            // вместо лицевой (рамка подсветит ошибку).
            gate    = SideGateConfig.frontOnly(),
            title   = "Сканируйте лицевую сторону",
            onResult = { scan ->
                frontScan = scan
                step = QuickstartStep.ScanBack
            },
        )

        QuickstartStep.ScanBack -> QuickstartScanScreen(
            side    = MIDSide.BACK,
            license = license,
            // Гейт поколения: обратная сторона обязана быть того же поколения
            // макета (2008/2017/2025), что и уже отсканированная лицевая —
            // защита от подмены стороны другим документом. Если лицевая пришла
            // без распознанного поколения — гейт работает только по стороне.
            gate    = frontScan?.generation?.raw
                          ?.let { SideGateConfig.back(it) }
                          ?: SideGateConfig.backOnly(),
            title   = "Сканируйте обратную сторону",
            onResult = { scan ->
                val front = frontScan
                if (front == null) {
                    step = QuickstartStep.ScanFront // защита от рассинхронизации
                } else {
                    startVerify(front, scan)
                }
            },
        )

        QuickstartStep.Verifying -> QuickstartVerifyingScreen()

        is QuickstartStep.Result -> QuickstartResultScreen(
            result = current.result,
            onRestart = {
                frontScan = null
                step = QuickstartStep.ScanFront
            },
        )

        is QuickstartStep.Error -> QuickstartErrorScreen(
            message = current.message,
            onRestart = {
                frontScan = null
                step = QuickstartStep.ScanFront
            },
        )
    }

    // ── Вариант с галереей ───────────────────────────────────────────────────
    // Любую сторону можно передать фотографией вместо камерного скана —
    // достаточно заменить MergenSideInput.Scan на MergenSideInput.Image:
    //
    //     val result = Mergen.verify(
    //         context = context,
    //         front   = MergenSideInput.Image(frontBitmap), // Bitmap из галереи
    //         back    = MergenSideInput.Scan(backScan),     // можно смешивать со сканами
    //         license = license,
    //     )
    //
    // Для выбора фото в SDK есть готовый rememberMergenGalleryLauncher —
    // он открывает системный Photo Picker и возвращает Bitmap с уже
    // исправленной EXIF-ориентацией (см. руководство, раздел «Галерея»).
}

// ── Экран сканирования (обе стороны) ─────────────────────────────────────────
// MergenScanView — готовый камерный сканер одной стороны: камера, рамка,
// подсказки и автозахват внутри. onResult приходит один раз, когда сторона
// успешно захвачена.
@Composable
private fun QuickstartScanScreen(
    side: MIDSide,
    license: String,
    gate: SideGateConfig,
    title: String,
    onResult: (MergenScanResult) -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        MergenScanView(
            side      = side,
            license   = license,
            sideGate  = gate,
            scanSpeed = ScanSpeed.NORMAL, // 3 кадра стабилизации — баланс скорости/надёжности
            onResult  = onResult,
        )

        // Небольшой баннер-подсказка сверху — какую сторону ждём.
        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 48.dp),
            shape = RoundedCornerShape(12.dp),
            color = Color(0xB3000000),
        ) {
            Text(
                text     = title,
                color    = Color.White,
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
            )
        }
    }
}

// ── Экран ожидания ───────────────────────────────────────────────────────────
@Composable
private fun QuickstartVerifyingScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(Modifier.height(16.dp))
            Text("Проверка документа…", fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(4.dp))
            Text(
                "Обычно занимает несколько секунд",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ── Экран результата ─────────────────────────────────────────────────────────
@Composable
private fun QuickstartResultScreen(result: VerifyResult, onRestart: () -> Unit) {
    // Успех = VERIFIED (MRZ подтверждён) или VERIFIED_VISUAL (только визуальная сверка).
    val isSuccess = result.status == VerifyStatus.VERIFIED ||
                    result.status == VerifyStatus.VERIFIED_VISUAL

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = if (isSuccess) "✔" else "✘",
            fontSize = 56.sp,
            color = if (isSuccess) Color(0xFF00C853) else MaterialTheme.colorScheme.error,
        )
        Spacer(Modifier.height(12.dp))

        Text(
            text = statusLabel(result.status),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )

        // Текст вердикта от SDK (заполнен при неуспехе) — можно показывать как есть.
        result.message?.takeIf { it.isNotBlank() }?.let { message ->
            Spacer(Modifier.height(8.dp))
            Text(
                text = message,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }

        Spacer(Modifier.height(20.dp))

        // Главные значения после MRZ-сверки: номер документа и ПИН.
        // Пустая строка = поле недоступно (не распознано / нет на этой версии карты).
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (result.verifyId.isNotBlank()) {
                    QuickstartFieldRow("Номер документа", result.verifyId)
                }
                if (result.verifyPin.isNotBlank()) {
                    QuickstartFieldRow("ПИН", result.verifyPin)
                }
                // Пер-сторонняя диагностика: что именно не так с конкретной стороной.
                result.frontIssue?.let {
                    QuickstartFieldRow("Лицевая сторона", issueLabel(it, expected = "лицевая"))
                }
                result.backIssue?.let {
                    QuickstartFieldRow("Обратная сторона", issueLabel(it, expected = "обратная"))
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Button(onClick = onRestart, modifier = Modifier.fillMaxWidth()) {
            Text("Сканировать заново")
        }
    }
}

// ── Экран ошибки ─────────────────────────────────────────────────────────────
@Composable
private fun QuickstartErrorScreen(message: String, onRestart: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("⚠", fontSize = 48.sp, color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(12.dp))
        Text(message, textAlign = TextAlign.Center) // уже готовый пользовательский текст
        Spacer(Modifier.height(24.dp))
        Button(onClick = onRestart, modifier = Modifier.fillMaxWidth()) {
            Text("Сканировать заново")
        }
    }
}

// ── Мелкие помощники ─────────────────────────────────────────────────────────
@Composable
private fun QuickstartFieldRow(label: String, value: String) {
    Column {
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
        )
    }
}

private fun statusLabel(status: VerifyStatus): String = when (status) {
    VerifyStatus.VERIFIED        -> "Подтверждено"
    VerifyStatus.VERIFIED_VISUAL -> "Подтверждено (визуально, без MRZ)"
    VerifyStatus.FAILED_FRONT    -> "Лицевая сторона не распознана"
    VerifyStatus.FAILED_BACK     -> "Обратная сторона не распознана"
    VerifyStatus.FAILED          -> "Не подтверждено"
    VerifyStatus.UNKNOWN         -> "Нет данных"
}

private fun issueLabel(issue: VerifySideIssue, expected: String): String = when (issue) {
    VerifySideIssue.WRONG_SIDE     -> "Ожидается $expected сторона"
    VerifySideIssue.NOT_RECOGNIZED -> "Сторона не распознана"
}
