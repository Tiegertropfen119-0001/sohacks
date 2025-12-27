package com.hackerman.sohacksrev2

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.CheckBox
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * Activity for scanning and selecting BLE devices.
 * 
 * This activity provides a user interface for discovering nearby Bluetooth Low Energy
 * devices. It handles runtime permissions, location services requirements, and provides
 * filtering options for the scan results.
 * 
 * Features:
 * - BLE device scanning with configurable duration
 * - Optional filtering of unnamed devices
 * - Automatic de-duplication of devices
 * - Permission and location services management
 * - Auto-stop scanning when device is selected or activity is paused
 */
class DeviceSelectionActivity1 : AppCompatActivity() {

    companion object {
        private const val TAG = "DeviceSelection"
        
        /** Duration of scan in milliseconds (12 seconds), set to 0 to disable auto-stop */
        private const val SCAN_PERIOD_MS = 12000L
        
        /** Request code for permission requests */
        private const val REQ_PERMS = 1001
        
        /** Request code for location services enable request */
        private const val REQ_ENABLE_LOCATION = 1002
    }

    private lateinit var rvDevices: RecyclerView
    private lateinit var btnBack: Button
    private lateinit var btnStartScan: Button
    private lateinit var btnStopScan: Button
    private var cbShowUnnamed: CheckBox? = null
    private lateinit var adapter: DeviceAdapter

    private var bluetoothAdapter: BluetoothAdapter? = null
    private val handler = Handler(Looper.getMainLooper())
    private var scanning = false

    /** Cache of all discovered devices, indexed by MAC address */
    private val allDevices = LinkedHashMap<String, Device>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_selection)

        initializeBluetoothAdapter()
        initializeViews()
        setupRecyclerView()
        setupButtonListeners()
    }

    /**
     * Initializes the Bluetooth adapter.
     * Logs an error if Bluetooth is not available on the device.
     */
    private fun initializeBluetoothAdapter() {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        bluetoothAdapter = bluetoothManager?.adapter
        
        if (bluetoothAdapter == null) {
            Log.e(TAG, "Bluetooth not available on this device")
            Toast.makeText(this, "Bluetooth not available", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Initializes all view references.
     */
    private fun initializeViews() {
        rvDevices = findViewById(R.id.rvDevices)
        btnBack = findViewById(R.id.btnBack)
        btnStartScan = findViewById(R.id.btnStartScan)
        btnStopScan = findViewById(R.id.btnStopScan)
        cbShowUnnamed = findViewById(R.id.cbShowUnnamed)

        btnStartScan.isEnabled = true
        btnStopScan.isEnabled = false
    }

    /**
     * Sets up the RecyclerView with the device adapter.
     */
    private fun setupRecyclerView() {
        adapter = DeviceAdapter(mutableListOf()) { device ->
            onDeviceSelected(device)
        }
        rvDevices.layoutManager = LinearLayoutManager(this)
        rvDevices.adapter = adapter
    }

    /**
     * Sets up click listeners for all buttons and checkboxes.
     */
    private fun setupButtonListeners() {
        btnBack.setOnClickListener { 
            stopScan()
            setResult(Activity.RESULT_CANCELED)
            finish()
        }
        
        btnStartScan.setOnClickListener { ensurePermissionsThenScan() }
        btnStopScan.setOnClickListener { stopScan() }
        cbShowUnnamed?.setOnCheckedChangeListener { _, _ -> refreshList() }
    }

    /**
     * Handles device selection by the user.
     * Stops scanning and returns the selected device address to the calling activity.
     * 
     * @param device The selected device
     */
    private fun onDeviceSelected(device: Device) {
        stopScan()
        val returnIntent = Intent().apply {
            putExtra("DEVICE_ADDRESS", device.address)
        }
        setResult(Activity.RESULT_OK, returnIntent)
        finish()
    }

    override fun onPause() {
        super.onPause()
        // Stop scanning when activity is paused to save battery
        stopScan()
    }

    /**
     * Ensures all required permissions are granted before starting the scan.
     * Requests permissions if needed and validates location services are enabled.
     */
    private fun ensurePermissionsThenScan() {
        val needed = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            needed += Manifest.permission.BLUETOOTH_SCAN
            needed += Manifest.permission.BLUETOOTH_CONNECT
        } else {
            needed += Manifest.permission.ACCESS_FINE_LOCATION
        }
        
        val toRequest = needed.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        
        if (toRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, toRequest.toTypedArray(), REQ_PERMS)
            return
        }
        
        // Many device manufacturers require location to be enabled for BLE scanning
        if (!isLocationEnabled()) {
            Toast.makeText(
                this,
                "Please enable location services (required for BLE scanning)",
                Toast.LENGTH_LONG
            ).show()
            startActivityForResult(
                Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS),
                REQ_ENABLE_LOCATION
            )
            return
        }
        
        startScan()
    }

    /**
     * Handles the result of permission requests.
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_PERMS) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                ensurePermissionsThenScan()
            } else {
                Toast.makeText(this, "Permissions required for scanning", Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * Handles the result of location services enable request.
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_ENABLE_LOCATION) {
            if (isLocationEnabled()) {
                startScan()
            } else {
                Toast.makeText(this, "Location services are still disabled", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Checks if location services are enabled.
     * 
     * @return true if location services are enabled, false otherwise
     */
    private fun isLocationEnabled(): Boolean {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        return try {
            locationManager?.isLocationEnabled ?: true
        } catch (e: Exception) {
            Log.e(TAG, "Error checking location status", e)
            true
        }
    }

    /**
     * Determines if unnamed devices should be shown in the list.
     * 
     * @return true if unnamed devices should be shown, false otherwise
     */
    private fun showUnnamedAllowed(): Boolean = cbShowUnnamed?.isChecked ?: true

    /**
     * Starts BLE device scanning with optimized settings.
     * Clears previous results and configures the scanner for maximum performance.
     */
    private fun startScan() {
        if (scanning) return
        
        val adapter = bluetoothAdapter
        if (adapter == null) {
            Toast.makeText(this, "Bluetooth adapter not available", Toast.LENGTH_SHORT).show()
            return
        }

        if (!adapter.isEnabled) {
            Toast.makeText(this, "Bluetooth is disabled", Toast.LENGTH_SHORT).show()
            return
        }

        val scanner = adapter.bluetoothLeScanner
        if (scanner == null) {
            Toast.makeText(this, "BLE scanner not available", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(this, "Scanning for devices…", Toast.LENGTH_SHORT).show()
        Log.d(TAG, "Starting BLE scan")

        // Clear previous results
        allDevices.clear()
        this.adapter.clear()

        // Configure scan settings for optimal performance
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY) // Maximum scan rate
            .setReportDelay(0L) // Report results immediately
            .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
            .setNumOfMatches(ScanSettings.MATCH_NUM_MAX_ADVERTISEMENT)
            .build()

        scanning = true
        btnStartScan.isEnabled = false
        btnStopScan.isEnabled = true

        try {
            scanner.startScan(null, settings, scanCallback)
            
            // Auto-stop after SCAN_PERIOD_MS if configured
            if (SCAN_PERIOD_MS > 0) {
                handler.postDelayed({ stopScan() }, SCAN_PERIOD_MS)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting scan", e)
            Toast.makeText(this, "Failed to start scan: ${e.message}", Toast.LENGTH_SHORT).show()
            scanning = false
            btnStartScan.isEnabled = true
            btnStopScan.isEnabled = false
        }
    }

    /**
     * Stops BLE device scanning.
     */
    private fun stopScan() {
        if (!scanning) return
        
        val scanner = bluetoothAdapter?.bluetoothLeScanner
        if (scanner != null) {
            try {
                scanner.stopScan(scanCallback)
                Log.d(TAG, "Scan stopped")
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping scan", e)
            }
        }
        
        scanning = false
        btnStartScan.isEnabled = true
        btnStopScan.isEnabled = false
    }

    /**
     * Refreshes the device list based on current filter settings.
     */
    private fun refreshList() {
        val allowUnnamed = showUnnamedAllowed()
        adapter.clear()
        
        for (device in allDevices.values) {
            if (allowUnnamed || device.hasName()) {
                adapter.addDevice(device)
            }
        }
    }

    /**
     * Callback for BLE scan results.
     */
    private val scanCallback = object : ScanCallback() {
        
        /**
         * Called when a BLE advertisement has been found.
         */
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            try {
                val bluetoothDevice = result.device ?: return
                val address = bluetoothDevice.address ?: return
                val name = bluetoothDevice.name

                val existing = allDevices[address]
                // Update if new device or if we now have a name for a previously unnamed device
                if (existing == null || (existing.name.isNullOrBlank() && !name.isNullOrBlank())) {
                    allDevices[address] = Device(bluetoothDevice, name, address)
                    refreshList()
                }
                
                Log.d(TAG, "Device found: name=$name, address=$address, rssi=${result.rssi}")
            } catch (e: Exception) {
                Log.e(TAG, "Error processing scan result", e)
            }
        }

        /**
         * Called when multiple BLE advertisements are reported at once (batch mode).
         */
        override fun onBatchScanResults(results: MutableList<ScanResult>) {
            try {
                var changed = false
                
                for (result in results) {
                    val bluetoothDevice = result.device ?: continue
                    val address = bluetoothDevice.address ?: continue
                    val name = bluetoothDevice.name
                    
                    val existing = allDevices[address]
                    if (existing == null || (existing.name.isNullOrBlank() && !name.isNullOrBlank())) {
                        allDevices[address] = Device(bluetoothDevice, name, address)
                        changed = true
                    }
                }
                
                if (changed) {
                    refreshList()
                }
                
                Log.d(TAG, "Batch scan results: ${results.size} devices")
            } catch (e: Exception) {
                Log.e(TAG, "Error processing batch scan results", e)
            }
        }

        /**
         * Called when the scan fails.
         */
        override fun onScanFailed(errorCode: Int) {
            Log.e(TAG, "Scan failed with error code: $errorCode")
            Toast.makeText(
                this@DeviceSelectionActivity1,
                "Scan failed (error: $errorCode)",
                Toast.LENGTH_SHORT
            ).show()
            scanning = false
            btnStartScan.isEnabled = true
            btnStopScan.isEnabled = false
        }
    }
}
