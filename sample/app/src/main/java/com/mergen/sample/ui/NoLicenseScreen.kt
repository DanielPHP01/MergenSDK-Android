package com.mergen.sample.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Экран-заглушка, который отображается, когда license.json отсутствует в assets/.
 *
 * Показывает понятные инструкции вместо краша — безопасно для demo-сборок
 * без лицензионного файла.
 */
@Composable
internal fun NoLicenseScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "⚠",
            fontSize = 56.sp,
            color = MaterialTheme.colorScheme.error,
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = "Лицензионный файл не найден",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = "Mergen SDK требует валидный license.json для инициализации движка.",
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(24.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Что нужно сделать:",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                )
                Text(
                    text = "1. Получите license.json от команды Mergen (sdk@mergen.kz).",
                    fontSize = 13.sp,
                )
                Text(
                    text = "2. Поместите файл в:",
                    fontSize = 13.sp,
                )
                Text(
                    text = "app/src/main/assets/license.json",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "3. Убедитесь, что поле app_id в лицензии совпадает с applicationId " +
                            "приложения (com.mergen.sample для этого sample).",
                    fontSize = 13.sp,
                )
                Text(
                    text = "4. Пересоберите проект (Build > Rebuild Project).",
                    fontSize = 13.sp,
                )
            }
        }
    }
}
