package com.example.myapp

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log

class AegisVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null

    companion object {
        const val ACTION_CONNECT = "com.example.myapp.ACTION_CONNECT"
        const val ACTION_DISCONNECT = "com.example.myapp.ACTION_DISCONNECT"
        const val EXTRA_CONFIG_JSON = "extra_config_json"
        var isRunning: Boolean = false
            private set
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CONNECT -> {
                val configJson = intent.getStringExtra(EXTRA_CONFIG_JSON) ?: "{}"
                startVpnTunnel(configJson)
            }
            ACTION_DISCONNECT -> {
                stopVpnTunnel()
            }
        }
        return START_STICKY
    }

    private fun startVpnTunnel(configJson: String) {
        if (vpnInterface != null) {
            stopVpnTunnel()
        }

        try {
            Log.d("AegisVPN", "Starting tunnel with config: $configJson")

            val builder = Builder()
                .setSession("AegisVPN VIP")
                .addAddress("10.66.0.2", 32)
                .addRoute("0.0.0.0", 0)
                .addDnsServer("1.1.1.1")
                .addDnsServer("8.8.8.8")
                .setMtu(1400)

            // ប្រសិនបើមាន app.build.gradle ប្រើ applicationId ដូចគ្នា អ្នកអាច addDisallowedApplication លើ app ខ្លួនឯងដើម្បីកុំឱ្យ loop
            try {
                builder.addDisallowedApplication(packageName)
            } catch (_: Exception) {
            }

            vpnInterface = builder.establish()
            isRunning = vpnInterface != null

            // កន្លែងនេះជាកន្លែងដែលអ្នកនឹងហៅ Core Engine (ឧ. sing-box / Xray / OpenVPN / SSH)
            // ដោយប្រើ vpnInterface?.detachFd() ឬ local proxy port

        } catch (e: Exception) {
            Log.e("AegisVPN", "Failed to start VpnService: ${e.message}", e)
            isRunning = false
        }
    }

    private fun stopVpnTunnel() {
        try {
            vpnInterface?.close()
        } catch (_: Exception) {
        } finally {
            vpnInterface = null
            isRunning = false
            stopSelf()
        }
    }

    override fun onDestroy() {
        stopVpnTunnel()
        super.onDestroy()
    }
}
