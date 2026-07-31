package com.mergen.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import com.mergen.sample.ui.NoLicenseScreen
import com.mergen.sample.ui.QuickstartScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Проверяем наличие license.json до рендера UI.
        // Mergen.loadLicense возвращает null, если файл отсутствует в assets/.
        val hasLicense = try {
            assets.open("license.json").use { it.available() > 0 }
        } catch (_: Exception) {
            false
        }

        setContent {
            MaterialTheme {
                if (hasLicense) {
                    QuickstartScreen()
                } else {
                    NoLicenseScreen()
                }
            }
        }
    }
}
