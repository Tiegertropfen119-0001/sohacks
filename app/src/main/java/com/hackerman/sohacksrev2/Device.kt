package com.hackerman.sohacksrev2

import android.bluetooth.BluetoothDevice

/**
 * Represents a Bluetooth device discovered during scanning.
 * 
 * This data class encapsulates information about a BLE device, including
 * its Bluetooth device object, display name, and MAC address.
 * 
 * @property device The underlying Android BluetoothDevice object
 * @property name The advertised name of the device (may be null for unnamed devices)
 * @property address The MAC address of the device (unique identifier)
 */
data class Device(
    val device: BluetoothDevice,
    val name: String?,
    val address: String
) {
    /**
     * Returns a display-friendly name for the device.
     * If the device has no advertised name, returns "Unknown Device".
     * 
     * @return A non-null display name
     */
    fun getDisplayName(): String {
        return name?.takeIf { it.isNotBlank() } ?: "Unknown Device"
    }

    /**
     * Checks if this device has an advertised name.
     * 
     * @return true if the device has a name, false otherwise
     */
    fun hasName(): Boolean {
        return !name.isNullOrBlank()
    }
}