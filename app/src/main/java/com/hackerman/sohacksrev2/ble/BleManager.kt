package com.hackerman.sohacksrev2.ble

import android.bluetooth.*
import android.content.Context
import android.util.Log
import java.util.UUID

/**
 * Manages Bluetooth Low Energy (BLE) connections and communication.
 * 
 * This class encapsulates all BLE-related operations including connection management,
 * service discovery, and characteristic read/write operations. It provides a clean
 * interface for the UI layer to interact with BLE devices without needing to handle
 * the low-level BLE API directly.
 */
class BleManager(private val context: Context) {
    
    companion object {
        private const val TAG = "BleManager"
        private val CCCD_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothGatt: BluetoothGatt? = null
    private var writeCharacteristic: BluetoothGattCharacteristic? = null
    private var notifyCharacteristic: BluetoothGattCharacteristic? = null
    
    private var connectionStateListener: ConnectionStateListener? = null
    private var dataReceivedListener: DataReceivedListener? = null

    init {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        bluetoothAdapter = bluetoothManager?.adapter
        
        if (bluetoothAdapter == null) {
            Log.e(TAG, "Bluetooth adapter not available on this device")
        }
    }

    /**
     * Listener for connection state changes.
     */
    interface ConnectionStateListener {
        fun onConnected()
        fun onDisconnected()
        fun onConnectionFailed(error: String)
        fun onServicesDiscovered(hasWriteAndNotify: Boolean)
    }

    /**
     * Listener for data received from the BLE device.
     */
    interface DataReceivedListener {
        fun onDataReceived(data: ByteArray)
    }

    /**
     * Sets the connection state listener.
     */
    fun setConnectionStateListener(listener: ConnectionStateListener?) {
        this.connectionStateListener = listener
    }

    /**
     * Sets the data received listener.
     */
    fun setDataReceivedListener(listener: DataReceivedListener?) {
        this.dataReceivedListener = listener
    }

    /**
     * Checks if the device is currently connected.
     * 
     * @return true if connected, false otherwise
     */
    fun isConnected(): Boolean {
        return bluetoothGatt != null && writeCharacteristic != null
    }

    /**
     * Connects to a BLE device with the given address.
     * 
     * @param deviceAddress The MAC address of the device to connect to
     * @throws IllegalArgumentException if the device address is invalid
     * @throws IllegalStateException if Bluetooth adapter is not available
     */
    fun connect(deviceAddress: String) {
        val adapter = bluetoothAdapter 
            ?: throw IllegalStateException("Bluetooth adapter not available")
        
        if (!adapter.isEnabled) {
            connectionStateListener?.onConnectionFailed("Bluetooth is not enabled")
            return
        }

        try {
            val device = adapter.getRemoteDevice(deviceAddress)
            Log.d(TAG, "Connecting to device: $deviceAddress")
            bluetoothGatt = device.connectGatt(context, false, gattCallback)
        } catch (e: IllegalArgumentException) {
            val errorMsg = "Invalid device address: $deviceAddress"
            Log.e(TAG, errorMsg, e)
            connectionStateListener?.onConnectionFailed(errorMsg)
        } catch (e: Exception) {
            val errorMsg = "Failed to connect: ${e.message}"
            Log.e(TAG, errorMsg, e)
            connectionStateListener?.onConnectionFailed(errorMsg)
        }
    }

    /**
     * Disconnects from the currently connected BLE device.
     */
    fun disconnect() {
        try {
            bluetoothGatt?.disconnect()
            bluetoothGatt?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error during disconnect: ${e.message}", e)
        } finally {
            bluetoothGatt = null
            writeCharacteristic = null
            notifyCharacteristic = null
        }
    }

    /**
     * Sends a hex string command to the connected device.
     * 
     * @param hexString The command as a hex string (e.g., "D707A45A00005")
     * @return true if the command was queued successfully, false otherwise
     */
    fun sendHexCommand(hexString: String): Boolean {
        val characteristic = writeCharacteristic
        if (characteristic == null) {
            Log.e(TAG, "Cannot send command: not connected or no write characteristic")
            return false
        }

        return try {
            val bytes = hexStringToByteArray(hexString)
            characteristic.value = bytes
            val success = bluetoothGatt?.writeCharacteristic(characteristic) ?: false
            
            if (success) {
                Log.d(TAG, "TX: $hexString")
            } else {
                Log.e(TAG, "Failed to write characteristic")
            }
            
            success
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Invalid hex string: $hexString", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error sending command: ${e.message}", e)
            false
        }
    }

    /**
     * Converts a hex string to a byte array.
     * 
     * @param hex The hex string to convert
     * @return The resulting byte array
     * @throws IllegalArgumentException if the hex string has invalid format
     */
    private fun hexStringToByteArray(hex: String): ByteArray {
        val cleaned = hex.replace(Regex("\\s"), "")
        
        if (cleaned.length % 2 != 0) {
            throw IllegalArgumentException("Hex string must have even length")
        }
        
        return try {
            ByteArray(cleaned.length / 2) { i ->
                cleaned.substring(i * 2, i * 2 + 2).toInt(16).toByte()
            }
        } catch (e: NumberFormatException) {
            throw IllegalArgumentException("Invalid hex string: contains non-hex characters", e)
        }
    }

    /**
     * Internal GATT callback for handling BLE events.
     */
    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.d(TAG, "Connected to GATT server")
                    connectionStateListener?.onConnected()
                    gatt.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.d(TAG, "Disconnected from GATT server")
                    writeCharacteristic = null
                    notifyCharacteristic = null
                    connectionStateListener?.onDisconnected()
                }
                else -> {
                    Log.w(TAG, "Connection state changed to: $newState with status: $status")
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG, "Service discovery failed with status: $status")
                connectionStateListener?.onServicesDiscovered(false)
                return
            }

            try {
                val (write, notify) = findWriteAndNotifyCharacteristics(gatt)
                writeCharacteristic = write
                notifyCharacteristic = notify

                // Enable notifications if notify characteristic was found
                notify?.let { enableNotifications(gatt, it) }

                val success = write != null && notify != null
                connectionStateListener?.onServicesDiscovered(success)
                
                if (success) {
                    Log.d(TAG, "UART-like characteristics found and configured")
                } else {
                    Log.w(TAG, "UART-like characteristics not found")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during service discovery: ${e.message}", e)
                connectionStateListener?.onServicesDiscovered(false)
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            try {
                val data = characteristic.value
                if (data != null && data.isNotEmpty()) {
                    val hex = data.joinToString("") { String.format("%02X", it) }
                    Log.d(TAG, "RX: $hex")
                    dataReceivedListener?.onDataReceived(data)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling characteristic change: ${e.message}", e)
            }
        }
    }

    /**
     * Finds write and notify characteristics in the discovered services.
     * 
     * @param gatt The GATT instance with discovered services
     * @return Pair of write and notify characteristics (may be null)
     */
    private fun findWriteAndNotifyCharacteristics(
        gatt: BluetoothGatt
    ): Pair<BluetoothGattCharacteristic?, BluetoothGattCharacteristic?> {
        var writeChar: BluetoothGattCharacteristic? = null
        var notifyChar: BluetoothGattCharacteristic? = null

        for (service in gatt.services) {
            for (characteristic in service.characteristics) {
                val properties = characteristic.properties

                // Check for write characteristic
                if (writeChar == null && 
                    (properties and BluetoothGattCharacteristic.PROPERTY_WRITE != 0 ||
                     properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE != 0)) {
                    writeChar = characteristic
                }

                // Check for notify characteristic
                if (notifyChar == null && 
                    (properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0)) {
                    notifyChar = characteristic
                }

                // If both found, we're done
                if (writeChar != null && notifyChar != null) {
                    return Pair(writeChar, notifyChar)
                }
            }
        }

        return Pair(writeChar, notifyChar)
    }

    /**
     * Enables notifications for a characteristic.
     * 
     * @param gatt The GATT instance
     * @param characteristic The characteristic to enable notifications for
     */
    private fun enableNotifications(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
        try {
            gatt.setCharacteristicNotification(characteristic, true)
            
            val descriptor = characteristic.getDescriptor(CCCD_UUID)
            if (descriptor != null) {
                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                gatt.writeDescriptor(descriptor)
                Log.d(TAG, "Notifications enabled for characteristic")
            } else {
                Log.w(TAG, "CCCD descriptor not found for characteristic")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error enabling notifications: ${e.message}", e)
        }
    }

    /**
     * Cleans up resources. Should be called when the manager is no longer needed.
     */
    fun cleanup() {
        disconnect()
        connectionStateListener = null
        dataReceivedListener = null
    }
}
