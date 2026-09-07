package com.example

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.ThemeMode
import com.example.ui.TtsReaderScreen
import com.example.ui.TtsReaderViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
        requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
      }
    }

    setContent {
      val viewModel: TtsReaderViewModel = viewModel()
      val uiState by viewModel.uiState.collectAsState()
      val systemDark = isSystemInDarkTheme()
      val isDark = when (uiState.themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
      }
      MyApplicationTheme(darkTheme = isDark) {
        TtsReaderScreen(viewModel = viewModel)
      }
    }
  }
}

