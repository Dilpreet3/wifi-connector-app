package com.example.wificonnector

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.*
import androidx.core.app.ActivityCompat

class MainActivity : AppCompatActivity() {

    private lateinit var wifiManager: WifiManager
    private val wifiList = ArrayList<ScanResult>()
    private lateinit var adapter: ArrayAdapter<String>
    private var selectedSSID: String? = null

    private lateinit var passwordInput: EditText
    private lateinit var statusText: TextView

    private val wifiScanReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)
            if (success) {
                updateListOfNetworks()
            } else {
                statusText.text = "Scan failed"
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        passwordInput = findViewById(R.id.passwordInput)
        statusText = findViewById(R.id.statusText)

        val wifiListView: ListView = findViewById(R.id.wifiListView)
        val scanBtn: Button = findViewById(R.id.scanBtn)
        val connectBtn: Button = findViewById(R.id.connectBtn)

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1)
        wifiListView.adapter = adapter

        wifiListView.onItemClickListener = { _, _, position, _ ->
            selectedSSID = "\"${wifiList[position].SSID}\""
            statusText.text = "Selected: ${wifiList[position].SSID}"
        }

        scanBtn.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
            } else {
                startScan()
            }
        }

        connectBtn.setOnClickListener {
            val password = passwordInput.text.toString()
            if (selectedSSID == null || password.isEmpty()) {
                Toast.makeText(this, "Select network and enter password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            connectToNetwork(selectedSSID!!, password)
        }
    }

    private fun startScan() {
        if (!wifiManager.isWifiEnabled) wifiManager.isWifiEnabled = true

        registerReceiver(wifiScanReceiver, IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))
        val success = wifiManager.startScan()
        if (!success) {
            statusText.text = "Scan failed"
        } else {
            statusText.text = "Scanning..."
        }
    }

    private fun updateListOfNetworks() {
        wifiList.clear()
        wifiList.addAll(wifiManager.scanResults)

        adapter.clear()
        for (result in wifiList) {
            adapter.add("${result.SSID} (${result.level} dBm)")
        }
        adapter.notifyDataSetChanged()
        statusText.text = "Found ${wifiList.size} networks"
    }

    private fun connectToNetwork(ssid: String, password: String) {
        val config = WifiConfiguration().apply {
            SSID = ssid
            preSharedKey = "\"$password\""
            status = WifiConfiguration.Status.ENABLED
            allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP)
            allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP)
            allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK)
            allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP)
            allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP)
            allowedProtocols.set(WifiConfiguration.Protocol.RSN)
        }

        val netId = wifiManager.addNetwork(config)
        wifiManager.disconnect()
        val success = wifiManager.enableNetwork(netId, true)
        if (success) {
            statusText.text = "Connecting to $ssid"
        } else {
            statusText.text = "Failed to connect"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(wifiScanReceiver)
    }
}
