package com.example.obdscanner

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlin.concurrent.thread

/**
 * Activité principale - Scanner OBD-II basique.
 *
 * Fonctionnalités : lecture des codes DTC, effacement des codes,
 * affichage des données en temps réel (RPM, température, vitesse).
 *
 * Limites assumées : protocole OBD-II standard uniquement (pas de
 * contrôle bidirectionnel, pas de codage ECU - fonctions propriétaires
 * réservées aux scanners professionnels).
 */
class MainActivity : AppCompatActivity() {

    private var elm: ELM327? = null
    private var selectedDevice: BluetoothDevice? = null
    private var liveDataRunning = false

    private lateinit var tvOutput: TextView
    private lateinit var btnLiveData: Button

    private val bluetoothPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN)
    } else {
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvOutput = findViewById(R.id.tvOutput)
        btnLiveData = findViewById(R.id.btnLiveData)

        findViewById<Button>(R.id.btnSelectDevice).setOnClickListener { selectDevice() }
        findViewById<Button>(R.id.btnConnect).setOnClickListener { connectDevice() }
        findViewById<Button>(R.id.btnReadCodes).setOnClickListener { readCodes() }
        findViewById<Button>(R.id.btnClearCodes).setOnClickListener { clearCodes() }
        btnLiveData.setOnClickListener { toggleLiveData() }

        checkPermissions()
    }

    private fun checkPermissions() {
        val missing = bluetoothPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), 100)
        }
    }

    /** Affiche la liste des appareils Bluetooth déjà appairés pour en choisir un. */
    private fun selectDevice() {
        val adapter = BluetoothAdapter.getDefaultAdapter()
        if (adapter == null) {
            toast("Bluetooth non disponible sur cet appareil.")
            return
        }
        if (!adapter.isEnabled) {
            toast("Veuillez activer le Bluetooth d'abord.")
            return
        }

        if (ContextCompat.checkSelfPermission(this, permissionForConnect())
            != PackageManager.PERMISSION_GRANTED
        ) {
            checkPermissions()
            return
        }

        val pairedDevices = adapter.bondedDevices.toList()
        if (pairedDevices.isEmpty()) {
            toast("Aucun appareil appairé. Faites d'abord l'appairage dans les paramètres Bluetooth du téléphone.")
            return
        }

        val names = pairedDevices.map { it.name ?: it.address }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Choisir le scanner ELM327")
            .setItems(names) { _, which ->
                selectedDevice = pairedDevices[which]
                toast("Appareil sélectionné : ${names[which]}")
            }
            .show()
    }

    private fun connectDevice() {
        val device = selectedDevice
        if (device == null) {
            toast("Sélectionnez d'abord un appareil (étape 1).")
            return
        }
        appendOutput("Connexion en cours...")
        thread {
            try {
                val newElm = ELM327(device)
                newElm.connect()
                elm = newElm
                runOnUiThread { appendOutput("Connecté au scanner.") }
            } catch (e: Exception) {
                runOnUiThread { appendOutput("Erreur de connexion : ${e.message}") }
            }
        }
    }

    private fun readCodes() {
        val currentElm = elm
        if (currentElm == null) {
            toast("Connectez-vous d'abord au scanner (étape 2).")
            return
        }
        appendOutput("Lecture des codes...")
        thread {
            try {
                val codes = currentElm.readDtcCodes()
                runOnUiThread {
                    if (codes.isEmpty()) {
                        appendOutput("Aucun code DTC détecté.")
                    } else {
                        appendOutput("${codes.size} code(s) trouvé(s) :")
                        codes.forEach { code ->
                            appendOutput("  $code - ${DtcCodes.describe(code)}")
                        }
                        appendOutput("Note : un code indique une zone à vérifier, pas forcément la pièce exacte à changer.")
                    }
                }
            } catch (e: Exception) {
                runOnUiThread { appendOutput("Erreur de lecture : ${e.message}") }
            }
        }
    }

    private fun clearCodes() {
        val currentElm = elm
        if (currentElm == null) {
            toast("Connectez-vous d'abord au scanner (étape 2).")
            return
        }
        AlertDialog.Builder(this)
            .setTitle("Confirmer")
            .setMessage("Effacer tous les codes DTC ?")
            .setPositiveButton("Oui") { _, _ ->
                thread {
                    try {
                        currentElm.clearDtcCodes()
                        runOnUiThread { appendOutput("Codes effacés.") }
                    } catch (e: Exception) {
                        runOnUiThread { appendOutput("Erreur : ${e.message}") }
                    }
                }
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun toggleLiveData() {
        val currentElm = elm
        if (currentElm == null) {
            toast("Connectez-vous d'abord au scanner (étape 2).")
            return
        }

        if (liveDataRunning) {
            liveDataRunning = false
            btnLiveData.text = "5. Données en temps réel (Start/Stop)"
            return
        }

        liveDataRunning = true
        btnLiveData.text = "5. Arrêter les données en temps réel"

        thread {
            while (liveDataRunning) {
                try {
                    val rpm = currentElm.getRpm()
                    val temp = currentElm.getCoolantTemp()
                    val speed = currentElm.getSpeed()
                    runOnUiThread {
                        tvOutput.text =
                            "RPM: ${rpm ?: "N/A"} tr/min\n" +
                            "Température moteur: ${temp ?: "N/A"} °C\n" +
                            "Vitesse: ${speed ?: "N/A"} km/h"
                    }
                    Thread.sleep(1000)
                } catch (e: Exception) {
                    runOnUiThread { appendOutput("Erreur live data : ${e.message}") }
                    liveDataRunning = false
                }
            }
        }
    }

    private fun permissionForConnect(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Manifest.permission.BLUETOOTH_CONNECT
        } else {
            Manifest.permission.ACCESS_FINE_LOCATION
        }
    }

    private fun appendOutput(text: String) {
        tvOutput.text = "${tvOutput.text}\n$text"
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        liveDataRunning = false
        elm?.close()
    }
}
