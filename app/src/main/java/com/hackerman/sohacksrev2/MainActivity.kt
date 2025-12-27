package com.hackerman.sohacksrev2

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.slider.Slider
import com.hackerman.sohacksrev2.ble.BleManager
import com.hackerman.sohacksrev2.ble.ScooterCommandRepository

/**
 * Main activity for controlling a scooter via Bluetooth Low Energy.
 * 
 * This activity provides a user interface for connecting to a scooter and sending
 * various control commands including mode changes, speed adjustments, and lock controls.
 * It uses the BleManager for all Bluetooth operations and ScooterCommandRepository
 * for command generation.
 * 
 * Features:
 * - BLE connection management with auto-reconnect
 * - Driving mode selection (ECO, Normal, Sport, Developer)
 * - Speed limit control (8-30 km/h)
 * - Lock/unlock functionality
 * - Advanced mode selection (0-254)
 * - Custom hex command input
 */
class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
        private const val PREFS_NAME = "BLE_Prefs"
        private const val PREF_DEVICE_ADDRESS = "device_address"
        private const val PREF_DISCLAIMER_ACCEPTED = "disclaimerAccepted"
        private const val REQUEST_PERMISSIONS = 2001
        private const val REQUEST_DEVICE_SELECTION = 2002
    }

    // BLE Manager
    private lateinit var bleManager: BleManager


    // UI Components
    private lateinit var btnConnect: MaterialButton
    private lateinit var btnECO: MaterialButton
    private lateinit var btnNormal: MaterialButton
    private lateinit var btnSport: MaterialButton
    private lateinit var btnDev: MaterialButton
    private lateinit var btnLock: MaterialButton
    private lateinit var btnUnlock: MaterialButton
    private lateinit var btnChangeDevice: MaterialButton

    private lateinit var btn8kmh: MaterialButton
    private lateinit var btn15kmh: MaterialButton
    private lateinit var btn20kmh: MaterialButton
    private lateinit var btn25kmh: MaterialButton
    private lateinit var btn30kmh: MaterialButton

    private lateinit var sliderSpeedModifier: Slider
    private lateinit var tvSpeedModifier: TextView
    private lateinit var switchAdvanced: Switch

    // Advanced controls
    private lateinit var spinnerModes: Spinner
    private lateinit var txtCmdHex: EditText
    private lateinit var btnSendHex: MaterialButton
    private lateinit var tvBleOutput: TextView

    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeComponents()
        initializeViews()
        setupListeners()
        
        showDisclaimerIfNeeded()
        ensurePermissions()
        autoReconnectToLastDevice()
    }

    /**
     * Initializes core components including BLE manager and preferences.
     */
    private fun initializeComponents() {
        bleManager = BleManager(this)
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        
        setupBleListeners()
    }

    /**
     * Sets up BLE manager listeners for connection state and data reception.
     */
    private fun setupBleListeners() {
        bleManager.setConnectionStateListener(object : BleManager.ConnectionStateListener {
            override fun onConnected() {
                runOnUiThread {
                    btnConnect.text = "Connected"
                    Log.d(TAG, "BLE connected")
                }
            }

            override fun onDisconnected() {
                runOnUiThread {
                    btnConnect.text = "Connect"
                    Log.d(TAG, "BLE disconnected")
                }
            }

            override fun onConnectionFailed(error: String) {
                runOnUiThread {
                    btnConnect.text = "Connect"
                    Toast.makeText(this@MainActivity, "Connection failed: $error", Toast.LENGTH_LONG).show()
                    Log.e(TAG, "Connection failed: $error")
                }
            }

            override fun onServicesDiscovered(hasWriteAndNotify: Boolean) {
                runOnUiThread {
                    if (hasWriteAndNotify) {
                        Toast.makeText(this@MainActivity, "Device ready", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@MainActivity, "Device not compatible", Toast.LENGTH_LONG).show()
                    }
                }
            }
        })

        bleManager.setDataReceivedListener(object : BleManager.DataReceivedListener {
            override fun onDataReceived(data: ByteArray) {
                val hex = data.joinToString("") { String.format("%02X", it) }
                runOnUiThread {
                    tvBleOutput.text = hex
                }
            }
        })
    }

    /**
     * Initializes all view references.
     */
    private fun initializeViews() {
        btnConnect = findViewById(R.id.btnConnect)
        btnECO = findViewById(R.id.btnECO)
        btnNormal = findViewById(R.id.btnNormal)
        btnSport = findViewById(R.id.btnSport)
        btnDev = findViewById(R.id.btnDev)
        btnLock = findViewById(R.id.btnLock)
        btnUnlock = findViewById(R.id.btnUnlock)
        btnChangeDevice = findViewById(R.id.btnChangeDevice)

        btn8kmh = findViewById(R.id.btn8kmh)
        btn15kmh = findViewById(R.id.btn15kmh)
        btn20kmh = findViewById(R.id.btn20kmh)
        btn25kmh = findViewById(R.id.btn25kmh)
        btn30kmh = findViewById(R.id.btn30kmh)

        sliderSpeedModifier = findViewById(R.id.sliderSpeedModifier)
        tvSpeedModifier = findViewById(R.id.tvSpeedModifier)

        switchAdvanced = findViewById(R.id.switchAdvanced)

        // Advanced controls
        spinnerModes = findViewById(R.id.advanced_dropdown_1_to_254)
        txtCmdHex = findViewById(R.id.txt_cmd_hex)
        btnSendHex = findViewById(R.id.btnSendHex)
        tvBleOutput = findViewById(R.id.tvBleOutput)
    }

    /**
     * Sets up click listeners for all UI components.
     */
    private fun setupListeners() {
        setupConnectionButtons()
        setupModeButtons()
        setupSpeedButtons()
        setupLockButtons()
        setupAdvancedControls()
        setupSpeedSlider()
    }

    /**
     * Sets up listeners for connection-related buttons.
     */
    private fun setupConnectionButtons() {
        btnConnect.setOnClickListener {
            if (bleManager.isConnected()) {
                disconnect()
            } else {
                pickDevice()
            }
        }

        btnChangeDevice.setOnClickListener {
            disconnect()
            pickDevice()
        }
    }

    /**
     * Sets up listeners for driving mode buttons.
     */
    private fun setupModeButtons() {
        btnECO.setOnClickListener {
            sendCommand(ScooterCommandRepository.getDrivingModeCommand(
                ScooterCommandRepository.DrivingMode.ECO
            ))
        }

        btnNormal.setOnClickListener {
            sendCommand(ScooterCommandRepository.getDrivingModeCommand(
                ScooterCommandRepository.DrivingMode.NORMAL
            ))
        }

        btnSport.setOnClickListener {
            sendCommand(ScooterCommandRepository.getDrivingModeCommand(
                ScooterCommandRepository.DrivingMode.SPORT
            ))
        }

        btnDev.setOnClickListener {
            sendCommand(ScooterCommandRepository.getDrivingModeCommand(
                ScooterCommandRepository.DrivingMode.DEVELOPER
            ))
        }
    }

    /**
     * Sets up listeners for speed preset buttons.
     */
    private fun setupSpeedButtons() {
        btn8kmh.setOnClickListener { setSpeed(8) }
        btn15kmh.setOnClickListener { setSpeed(15) }
        btn20kmh.setOnClickListener { setSpeed(20) }
        btn25kmh.setOnClickListener { setSpeed(25) }
        btn30kmh.setOnClickListener { setSpeed(30) }
    }

    /**
     * Sets up listeners for lock control buttons.
     */
    private fun setupLockButtons() {
        btnLock.setOnClickListener {
            sendCommand(ScooterCommandRepository.getLockCommand(
                ScooterCommandRepository.LockState.LOCKED
            ))
        }

        btnUnlock.setOnClickListener {
            sendCommand(ScooterCommandRepository.getLockCommand(
                ScooterCommandRepository.LockState.UNLOCKED
            ))
        }
    }

    /**
     * Sets up the speed slider and its listener.
     */
    private fun setupSpeedSlider() {
        sliderSpeedModifier.addOnChangeListener { _, value, _ ->
            val speed = value.toInt().coerceIn(8, 30)
            tvSpeedModifier.text = "Speed Modifier: $speed km/h"
            setSpeed(speed)
        }
    }

    /**
     * Sets up advanced controls including mode spinner and hex input.
     */
    private fun setupAdvancedControls() {
        val advancedContainer = findViewById<androidx.core.widget.NestedScrollView>(R.id.advancedContainer)

        switchAdvanced.setOnCheckedChangeListener { _, isChecked ->
            advancedContainer.visibility = if (isChecked) View.VISIBLE else View.GONE
            advancedContainer.requestLayout()
        }

        // Setup mode spinner for advanced mode selection
        // Populates dropdown with modes 0-254, matching ScooterCommandRepository's supported range
        val modeLabels = (0..254).map { "Mode $it" }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, modeLabels)
        spinnerModes.adapter = adapter
        
        spinnerModes.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (!switchAdvanced.isChecked) return
                
                val command = ScooterCommandRepository.getAdvancedModeCommand(position)
                if (command != null) {
                    sendCommand(command)
                    Toast.makeText(this@MainActivity, "Sent mode $position", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@MainActivity, "Invalid mode: $position", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Setup hex input
        btnSendHex.setOnClickListener {
            val hex = txtCmdHex.text.toString().trim()
            if (hex.isEmpty()) {
                Toast.makeText(this, "Please enter a hex command", Toast.LENGTH_SHORT).show()
            } else {
                sendCommand(hex)
            }
        }
    }

    /**
     * Shows the disclaimer dialog if not previously accepted.
     */
    private fun showDisclaimerIfNeeded() {
        val accepted = prefs.getBoolean(PREF_DISCLAIMER_ACCEPTED, false)
        if (accepted) return

        MaterialAlertDialogBuilder(this)
            .setTitle("Important Notice / Disclaimer")
            .setMessage(
                "This app can modify your scooter's behavior. Tuning and altered speeds may be " +
                "illegal on public roads and can lead to fines, loss of insurance, or safety hazards.\n\n" +
                "This app is provided without warranty. The developer assumes no liability for damages, " +
                "legal consequences, or malfunctions. Use this app solely at your own responsibility and " +
                "only where legally permitted."
            )
            .setPositiveButton("I Accept the Risk") { _, _ ->
                prefs.edit().putBoolean(PREF_DISCLAIMER_ACCEPTED, true).apply()
            }
            .setNegativeButton("Cancel") { _, _ -> finish() }
            .setCancelable(false)
            .show()
    }

    /**
     * Ensures all required permissions are granted.
     */
    private fun ensurePermissions() {
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
            ActivityCompat.requestPermissions(this, toRequest.toTypedArray(), REQUEST_PERMISSIONS)
        }
    }

    /**
     * Attempts to automatically reconnect to the last connected device.
     */
    private fun autoReconnectToLastDevice() {
        val address = prefs.getString(PREF_DEVICE_ADDRESS, null) ?: return
        
        btnConnect.text = "Connecting…"
        
        try {
            bleManager.connect(address)
            Log.d(TAG, "Auto-reconnecting to: $address")
        } catch (e: Exception) {
            Log.e(TAG, "Auto-reconnect failed", e)
            btnConnect.text = "Connect"
            Toast.makeText(this, "Auto-reconnect failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Launches the device selection activity.
     */
    private fun pickDevice() {
        val intent = Intent(this, DeviceSelectionActivity1::class.java)
        startActivityForResult(intent, REQUEST_DEVICE_SELECTION)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == REQUEST_DEVICE_SELECTION && resultCode == Activity.RESULT_OK) {
            val address = data?.getStringExtra("DEVICE_ADDRESS")
            
            if (address != null) {
                prefs.edit().putString(PREF_DEVICE_ADDRESS, address).apply()
                btnConnect.text = "Connecting…"
                
                try {
                    bleManager.connect(address)
                } catch (e: Exception) {
                    Log.e(TAG, "Connection failed", e)
                    btnConnect.text = "Connect"
                    Toast.makeText(this, "Connection failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    /**
     * Disconnects from the currently connected device.
     */
    private fun disconnect() {
        bleManager.disconnect()
        btnConnect.text = "Connect"
        Toast.makeText(this, "Disconnected", Toast.LENGTH_SHORT).show()
    }

    /**
     * Sends a speed limit command to the scooter.
     * 
     * @param speedKmh The desired speed in km/h (8-30)
     */
    private fun setSpeed(speedKmh: Int) {
        if (!ScooterCommandRepository.isValidSpeed(speedKmh)) {
            Toast.makeText(this, "Invalid speed: $speedKmh km/h", Toast.LENGTH_SHORT).show()
            return
        }
        
        sendCommand(ScooterCommandRepository.getSpeedLimitCommand(speedKmh))
    }

    /**
     * Sends a hex command to the connected scooter.
     * 
     * @param hexCommand The hex command string to send
     */
    private fun sendCommand(hexCommand: String) {
        if (!bleManager.isConnected()) {
            Toast.makeText(this, "Not connected", Toast.LENGTH_SHORT).show()
            return
        }

        val success = bleManager.sendHexCommand(hexCommand)
        if (!success) {
            Toast.makeText(this, "Failed to send command", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        bleManager.cleanup()
    }
}
