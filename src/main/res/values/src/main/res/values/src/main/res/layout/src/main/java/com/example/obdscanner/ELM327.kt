package com.example.obdscanner

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

/**
 * Classe qui gère la connexion Bluetooth (SPP - Serial Port Profile)
 * vers un scanner ELM327 et l'envoi des commandes OBD-II standard.
 *
 * Protocole utilisé : OBD-II (SAE J1979), norme ouverte et publique.
 * Ne gère PAS le contrôle bidirectionnel ni le codage ECU (protocoles
 * propriétaires des constructeurs / fabricants de scanners professionnels).
 */
class ELM327(private val device: BluetoothDevice) {

    // UUID standard pour le profil série Bluetooth (SPP) - utilisé par presque tous les ELM327
    private val sppUuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    private var socket: BluetoothSocket? = null
    private var input: InputStream? = null
    private var output: OutputStream? = null

    /** Ouvre la connexion Bluetooth et initialise le scanner. */
    @Throws(Exception::class)
    fun connect() {
        socket = device.createRfcommSocketToServiceRecord(sppUuid)
        socket?.connect()
        input = socket?.inputStream
        output = socket?.outputStream

        // Séquence d'initialisation standard ELM327
        sendCommand("ATZ")    // Reset
        sendCommand("ATE0")   // Désactive l'echo
        sendCommand("ATL0")   // Désactive les retours à la ligne inutiles
        sendCommand("ATS0")   // Désactive les espaces inutiles
        sendCommand("ATSP0")  // Détection automatique du protocole OBD-II
    }

    /** Envoie une commande AT ou un PID et retourne la réponse brute. */
    @Throws(Exception::class)
    fun sendCommand(cmd: String): String {
        output?.write((cmd + "\r").toByteArray())
        output?.flush()
        Thread.sleep(300)

        val buffer = ByteArray(1024)
        val available = input?.available() ?: 0
        val bytesRead = if (available > 0) input?.read(buffer, 0, available) ?: 0 else 0
        return String(buffer, 0, bytesRead).replace(">", "").trim()
    }

    /** Lit les codes DTC enregistrés (Mode 03). */
    fun readDtcCodes(): List<String> {
        val raw = sendCommand("03")
        return parseDtc(raw)
    }

    /** Efface tous les codes DTC (Mode 04). À utiliser avec prudence. */
    fun clearDtcCodes(): String {
        return sendCommand("04")
    }

    /** Lit le régime moteur (RPM) - PID 0C. */
    fun getRpm(): Int? {
        val raw = sendCommand("010C").replace(" ", "").replace("\r", "")
        val idx = raw.indexOf("410C")
        if (idx == -1 || raw.length < idx + 8) return null
        return try {
            val a = raw.substring(idx + 4, idx + 6).toInt(16)
            val b = raw.substring(idx + 6, idx + 8).toInt(16)
            ((a * 256) + b) / 4
        } catch (e: Exception) {
            null
        }
    }

    /** Lit la température du liquide de refroidissement (°C) - PID 05. */
    fun getCoolantTemp(): Int? {
        val raw = sendCommand("0105").replace(" ", "").replace("\r", "")
        val idx = raw.indexOf("4105")
        if (idx == -1 || raw.length < idx + 6) return null
        return try {
            raw.substring(idx + 4, idx + 6).toInt(16) - 40
        } catch (e: Exception) {
            null
        }
    }

    /** Lit la vitesse du véhicule (km/h) - PID 0D. */
    fun getSpeed(): Int? {
        val raw = sendCommand("010D").replace(" ", "").replace("\r", "")
        val idx = raw.indexOf("410D")
        if (idx == -1 || raw.length < idx + 6) return null
        return try {
            raw.substring(idx + 4, idx + 6).toInt(16)
        } catch (e: Exception) {
            null
        }
    }

    private fun parseDtc(raw: String): List<String> {
        val codes = mutableListOf<String>()
        var cleaned = raw.replace("\r", "").replace("\n", "").replace(" ", "")
        if (cleaned.startsWith("43")) cleaned = cleaned.substring(2)

        var i = 0
        while (i + 4 <= cleaned.length) {
            val chunk = cleaned.substring(i, i + 4)
            if (chunk != "0000") {
                decodeDtc(chunk)?.let { codes.add(it) }
            }
            i += 4
        }
        return codes
    }

    private fun decodeDtc(chunk: String): String? {
        return try {
            val firstByte = chunk[0].toString().toInt(16)
            val prefix = when (firstByte shr 2) {
                0 -> "P0"; 1 -> "P1"; 2 -> "P2"; 3 -> "P3"
                4 -> "C0"; 5 -> "C1"; 6 -> "C2"; 7 -> "C3"
                8 -> "B0"; 9 -> "B1"; 10 -> "B2"; 11 -> "B3"
                12 -> "U0"; 13 -> "U1"; 14 -> "U2"; else -> "U3"
            }
            val remainder = ((firstByte and 0x03) shl 8) or chunk[1].toString().toInt(16)
            "$prefix${Integer.toHexString(remainder).uppercase()}${chunk[2]}${chunk[3]}".uppercase()
        } catch (e: Exception) {
            null
        }
    }

    /** Ferme proprement la connexion. */
    fun close() {
        try {
            input?.close()
            output?.close()
            socket?.close()
        } catch (e: Exception) {
            // Ignoré volontairement lors de la fermeture
        }
    }
}
