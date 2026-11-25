package com.example.myapp

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private var pendingConfigJson: String = "{}"

    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            launchVpnService(pendingConfigJson)
            notifyJsVpnState(true, "VPN_PERMISSION_GRANTED")
        } else {
            notifyJsVpnState(false, "VPN_PERMISSION_DENIED")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webView = WebView(this)
        setContentView(webView)

        val webSettings: WebSettings = webView.settings
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true
        webSettings.allowFileAccess = true

        webView.addJavascriptInterface(AndroidBridge(), "AndroidBridge")
        webView.webViewClient = WebViewClient()
        webView.loadUrl("file:///android_asset/index.html")
    }

    private fun requestOrStartVpn(configJson: String) {
        pendingConfigJson = configJson
        val prepareIntent = VpnService.prepare(this)
        if (prepareIntent != null) {
            vpnPermissionLauncher.launch(prepareIntent)
        } else {
            launchVpnService(configJson)
            notifyJsVpnState(true, "VPN_CONNECTED")
        }
    }

    private fun launchVpnService(configJson: String) {
        val intent = Intent(this, AegisVpnService::class.java).apply {
            action = AegisVpnService.ACTION_CONNECT
            putExtra(AegisVpnService.EXTRA_CONFIG_JSON, configJson)
        }
        startService(intent)
    }

    private fun stopVpnService() {
        val intent = Intent(this, AegisVpnService::class.java).apply {
            action = AegisVpnService.ACTION_DISCONNECT
        }
        startService(intent)
        notifyJsVpnState(false, "VPN_DISCONNECTED")
    }

    private fun notifyJsVpnState(connected: Boolean, statusCode: String) {
        runOnUiThread {
            val jsCode = "if (window.onNativeVpnState) { window.onNativeVpnState($connected, '$statusCode'); }"
            webView.evaluateJavascript(jsCode, null)
        }
    }

    inner class AndroidBridge {
        @JavascriptInterface
        fun connectVpn(configJson: String) {
            runOnUiThread {
                requestOrStartVpn(configJson)
            }
        }

        @JavascriptInterface
        fun disconnectVpn() {
            runOnUiThread {
                stopVpnService()
            }
        }

        @JavascriptInterface
        fun isVpnActive(): Boolean {
            return AegisVpnService.isRunning
        }
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
