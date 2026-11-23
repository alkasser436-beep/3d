package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ai.configureAppCheck
import com.example.ui.DesignerScreen
import com.example.ui.theme.RoomCraftTheme
import com.example.viewmodel.DesignerViewModel

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Must run before setContent, ViewModels, or Firebase AI calls so App Check debug/release tokens are ready.
    configureAppCheck(this, intent)

    enableEdgeToEdge()
    setContent {
      RoomCraftTheme {
        val vm: DesignerViewModel = viewModel()
        DesignerScreen(viewModel = vm)
      }
    }
  }
}
