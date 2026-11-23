package com.example.ui.components

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.DesignSerializer
import com.example.model.CameraPreset
import com.example.model.RoomDesign
import com.example.model.SelectionTarget
import kotlinx.coroutines.flow.SharedFlow
import org.json.JSONObject

private class DesignerBridge(
  private val onReadyCallback: () -> Unit,
  private val onElementSelectedCallback: (String, String) -> Unit,
  private val onSnapshotCapturedCallback: (String) -> Unit,
) {
  @JavascriptInterface
  fun onReady() {
    onReadyCallback()
  }

  @JavascriptInterface
  fun onElementSelected(type: String, id: String) {
    onElementSelectedCallback(type, id)
  }

  @JavascriptInterface
  fun onSnapshotCaptured(dataUrl: String) {
    onSnapshotCapturedCallback(dataUrl)
  }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun Viewport3D(
  design: RoomDesign,
  selectedTarget: SelectionTarget,
  cameraCommands: SharedFlow<CameraPreset>,
  snapshotRequests: SharedFlow<Int>,
  onElementSelected: (String, String) -> Unit,
  onSnapshotCaptured: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  var webViewRef by remember { mutableStateOf<WebView?>(null) }
  var isPageReady by remember { mutableStateOf(false) }

  val currentOnSelect by rememberUpdatedState(onElementSelected)
  val currentOnSnap by rememberUpdatedState(onSnapshotCaptured)
  val currentDesign by rememberUpdatedState(design)
  val currentTarget by rememberUpdatedState(selectedTarget)

  LaunchedEffect(design, selectedTarget, isPageReady) {
    val wv = webViewRef ?: return@LaunchedEffect
    if (!isPageReady) return@LaunchedEffect
    val payload = DesignSerializer.toViewportPayload(design, selectedTarget)
    val quoted = JSONObject.quote(payload)
    wv.evaluateJavascript("window.updateScene($quoted);", null)
  }

  LaunchedEffect(webViewRef, isPageReady) {
    cameraCommands.collect { preset ->
      val wv = webViewRef ?: return@collect
      if (isPageReady) {
        val q = JSONObject.quote(preset.name)
        wv.evaluateJavascript("window.setCameraPreset($q);", null)
      }
    }
  }

  LaunchedEffect(snapshotRequests) {
    snapshotRequests.collect { maxDim ->
      var waitedMs = 0
      while ((webViewRef == null || !isPageReady) && waitedMs < 3500) {
        kotlinx.coroutines.delay(100L)
        waitedMs += 100
      }
      val wv = webViewRef
      if (wv != null && isPageReady) {
        wv.evaluateJavascript("window.captureSnapshot($maxDim);", null)
      } else {
        currentOnSnap("")
      }
    }
  }

  DisposableEffect(Unit) {
    onDispose {
      webViewRef?.destroy()
      webViewRef = null
    }
  }

  Box(modifier = modifier) {
    AndroidView(
      modifier = Modifier.fillMaxSize(),
      factory = { context ->
        WebView(context).apply {
          layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
          )
          setLayerType(View.LAYER_TYPE_HARDWARE, null)
          settings.javaScriptEnabled = true
          settings.domStorageEnabled = true
          settings.allowFileAccess = true
          settings.allowContentAccess = true
          settings.cacheMode = WebSettings.LOAD_NO_CACHE
          webChromeClient = WebChromeClient()
          addJavascriptInterface(
            DesignerBridge(
              onReadyCallback = {
                post {
                  isPageReady = true
                  val payload = DesignSerializer.toViewportPayload(currentDesign, currentTarget)
                  evaluateJavascript("window.updateScene(${JSONObject.quote(payload)});", null)
                }
              },
              onElementSelectedCallback = { type, id ->
                post { currentOnSelect(type, id) }
              },
              onSnapshotCapturedCallback = { dataUrl ->
                post { currentOnSnap(dataUrl) }
              },
            ),
            "AndroidBridge",
          )
          webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
              super.onPageFinished(view, url)
              isPageReady = true
              val payload = DesignSerializer.toViewportPayload(currentDesign, currentTarget)
              view?.evaluateJavascript("window.updateScene(${JSONObject.quote(payload)});", null)
            }
          }
          loadUrl("file:///android_asset/designer3d.html")
          webViewRef = this
        }
      },
    )
  }
}
