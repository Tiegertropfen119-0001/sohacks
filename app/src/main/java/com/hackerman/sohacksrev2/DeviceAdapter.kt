package com.hackerman.sohacksrev2

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/**
 * RecyclerView adapter for displaying BLE devices in a list.
 * 
 * This adapter manages the display of discovered Bluetooth devices, handling
 * dynamic updates as new devices are found during scanning. It supports
 * click events for device selection and automatic de-duplication based on
 * device MAC address.
 * 
 * @property devices Mutable list of discovered devices
 * @property onItemClick Callback invoked when a device is clicked
 */
class DeviceAdapter(
    private val devices: MutableList<Device>,
    private val onItemClick: (Device) -> Unit
) : RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder>() {

    /**
     * ViewHolder for device list items.
     * 
     * Holds references to the views for displaying device information.
     * 
     * @property itemView The root view of the list item
     */
    class DeviceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDeviceName: TextView = itemView.findViewById(R.id.tvDeviceName)
        val tvDeviceAddress: TextView = itemView.findViewById(R.id.tvDeviceAddress)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_device, parent, false)
        return DeviceViewHolder(view)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val device = devices[position]
        holder.tvDeviceName.text = device.getDisplayName()
        holder.tvDeviceAddress.text = device.address
        holder.itemView.setOnClickListener { onItemClick(device) }
    }

    override fun getItemCount(): Int = devices.size

    /**
     * Adds a device to the list if it's not already present.
     * 
     * Devices are considered duplicates if they have the same MAC address.
     * This method automatically notifies the adapter of the insertion.
     * 
     * @param device The device to add
     */
    fun addDevice(device: Device) {
        if (!devices.any { it.address == device.address }) {
            devices.add(device)
            notifyItemInserted(devices.size - 1)
        }
    }

    /**
     * Clears all devices from the list.
     * 
     * This method notifies the adapter that the entire dataset has changed.
     */
    fun clear() {
        devices.clear()
        notifyDataSetChanged()
    }
}