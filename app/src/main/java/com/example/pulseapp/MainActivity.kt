package com.example.pulseapp

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.pulseapp.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private val bluetoothAdapter: BluetoothAdapter? by lazy(LazyThreadSafetyMode.NONE) {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkPermissions()

        initConnectDeviceSpinner()
    }

    private val REQUEST_MULTI_PERMISSIONS = 101

    private fun initConnectDeviceSpinner() {
        Log.d("bluetooth", "function called")
        val btSpinner = findViewById<Spinner>(R.id.spinner)
        if(ActivityCompat.checkSelfPermission(this,Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Log.d("bluetooth","permission error")
            return
        }
        val pairedDevices = bluetoothAdapter?.bondedDevices

        Log.d("bluetooth","bondedDeviceOk")

        if(pairedDevices != null) {
            val btDeviceAdapter = ArrayAdapter<String>(this,android.R.layout.simple_spinner_item)

            if(pairedDevices.isEmpty()) {
                btDeviceAdapter.add("CONNECT_DEVICE_NONE")
            }
            else {
                pairedDevices.forEach {
                    val deviceName = it.name
                    btDeviceAdapter.add(deviceName)
                }
            }

            btSpinner.adapter = btDeviceAdapter
        }
    }

    private fun checkPermissions() {
        val requestPermissions = mutableListOf<String>()

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN,) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions.add(Manifest.permission.BLUETOOTH_SCAN)
        }


        if(requestPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, requestPermissions.toTypedArray(), REQUEST_MULTI_PERMISSIONS)
        }
    }
}