package com.aidebate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.runtime.CompositionLocalProvider
import com.aidebate.domain.repository.SettingsRepository
import com.aidebate.presentation.localization.*
import com.aidebate.presentation.navigation.AppNavHost
import com.aidebate.presentation.theme.AiDebateTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val langFlow = remember { settingsRepository.observeString(KEY_LANGUAGE) }
            val language by langFlow.collectAsState(initial = null)
            val translation = when (language) {
                LANG_CHINESE -> ChineseTranslation
                else -> EnglishTranslation
            }
            AiDebateTheme {
                CompositionLocalProvider(LocalTranslation provides translation) {
                    AppNavHost()
                }
            }
        }
    }
}
