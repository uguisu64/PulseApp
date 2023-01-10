package com.example.pulseapp

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.*
import android.util.Log
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.FileOutputStream
import java.io.InputStream
import java.time.LocalDateTime
import java.util.UUID

class MainActivity : AppCompatActivity() {

    lateinit var btSocket: BluetoothSocket
    lateinit var btInputStream: InputStream

    lateinit var file: String
    var fileSaveFlg = false

    val handler = Handler(Looper.getMainLooper())

    private val bluetoothAdapter: BluetoothAdapter? by lazy(LazyThreadSafetyMode.NONE) {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val connectButton = findViewById<Button>(R.id.connectButton)
        val recordButton = findViewById<Button>(R.id.recordButton)
        val stopButton = findViewById<Button>(R.id.stopButton)

        checkPermissions()

        initConnectDeviceSpinner()

        connectButton.setOnClickListener { connectDevice() }
        recordButton.setOnClickListener { recording() }
        stopButton.setOnClickListener { stopRecording() }
    }

    private val REQUEST_MULTI_PERMISSIONS = 101

    private fun initConnectDeviceSpinner() {
        if(ActivityCompat.checkSelfPermission(this,Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        val pairedDevices = bluetoothAdapter?.bondedDevices

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

            val btSpinner = findViewById<Spinner>(R.id.spinner)
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
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }


        if(requestPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, requestPermissions.toTypedArray(), REQUEST_MULTI_PERMISSIONS)
        }
    }

    private fun connectDevice() {
        if(ActivityCompat.checkSelfPermission(this,Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        //接続したいデバイスが存在するかの確認
        var connectBluetoothDevice: BluetoothDevice? = null

        val btSpinner = findViewById<Spinner>(R.id.spinner)
        val deviceName = btSpinner.getItemAtPosition(btSpinner.selectedItemPosition)
        val pairedDevice = bluetoothAdapter?.bondedDevices ?: return

        pairedDevice.forEach {
            if(deviceName.equals(it.name)) {
                connectBluetoothDevice = it
                return@forEach
            }
        }

        //接続部分
        val uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")
        btSocket = connectBluetoothDevice!!.createRfcommSocketToServiceRecord(uuid)
        btSocket.connect()
        btInputStream = btSocket.inputStream

        val btReceive = BtReceive(this)
        btReceive.start()
    }

    private fun recording() {
        val filename = findViewById<EditText>(R.id.editTextTextFileName).text
        if(filename.isNullOrEmpty()) {
            Toast.makeText(applicationContext, "enter file name!", Toast.LENGTH_SHORT).show()
        }
        else {
            val path = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS).toString()
            file = "${path}/${filename}.csv"
            Log.d("file", file)
            fileSaveFlg = true
        }
    }

    private fun stopRecording() {
        fileSaveFlg = false
    }


    class BtReceive(private val mainActivity: MainActivity): Thread() {
        private var isKeepRun = true
        @RequiresApi(Build.VERSION_CODES.O)
        override fun run() {
            isKeepRun = true
            while(isKeepRun) {
                receiveData()
                sleep(300)
            }
        }

        fun shutdown() {
            isKeepRun = false
        }

        @RequiresApi(Build.VERSION_CODES.O)
        private fun receiveData() {
            var receiveData = ByteArray(256)
            val count = mainActivity.btInputStream.read(receiveData)
            if(count > 0) {
                receiveData[count] = 0
                val receiveString = String(receiveData)
                setDataText(receiveString)
                if(mainActivity.fileSaveFlg) {
                    saveDataText(receiveString, LocalDateTime.now())
                }
            }
        }

        private fun setDataText(text: String) {
            mainActivity.handler.post {
                val dataText = mainActivity.findViewById<TextView>(R.id.textView)
                dataText.text = text
            }
        }

        private fun saveDataText(text: String, timeNow: LocalDateTime) {
            val texts = text.split("\n")
            if(texts[0].split(",").count() < 2) {
                return
            }

            if(Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
                val fileOutputStream = FileOutputStream(mainActivity.file, true)
                fileOutputStream.write("${timeNow},${texts[0]}\n".toByteArray())
            }
        }
    }
}