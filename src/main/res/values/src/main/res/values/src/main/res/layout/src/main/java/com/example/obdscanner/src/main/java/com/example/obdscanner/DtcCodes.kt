package com.example.obdscanner

/**
 * Liste basique des codes DTC (Diagnostic Trouble Codes) les plus courants
 * et leur description. Cette liste n'est pas exhaustive (il existe des
 * milliers de codes selon la marque et le modèle) mais couvre les cas
 * fréquents sur la plupart des véhicules.
 */
object DtcCodes {

    private val descriptions = mapOf(
        // Ratés d'allumage / injection
        "P0300" to "Ratés d'allumage non spécifiés (plusieurs cylindres)",
        "P0301" to "Raté d'allumage cylindre 1",
        "P0302" to "Raté d'allumage cylindre 2",
        "P0303" to "Raté d'allumage cylindre 3",
        "P0304" to "Raté d'allumage cylindre 4",

        // Richesse du mélange
        "P0171" to "Mélange trop pauvre (Bank 1) - fuite d'air ou capteur MAF",
        "P0172" to "Mélange trop riche (Bank 1)",
        "P0174" to "Mélange trop pauvre (Bank 2)",
        "P0175" to "Mélange trop riche (Bank 2)",

        // Capteurs
        "P0100" to "Problème capteur MAF (débit d'air)",
        "P0101" to "Signal MAF hors plage",
        "P0110" to "Problème capteur température d'air (IAT)",
        "P0115" to "Problème capteur température moteur (ECT)",
        "P0120" to "Problème capteur position papillon (TPS)",
        "P0130" to "Problème capteur oxygène (O2) Bank 1 Sensor 1",
        "P0135" to "Problème chauffage capteur O2 Bank 1 Sensor 1",

        // Allumage
        "P0325" to "Problème capteur de cliquetis",
        "P0335" to "Problème capteur vilebrequin",
        "P0340" to "Problème capteur arbre à cames",

        // Émissions
        "P0400" to "Débit EGR hors plage",
        "P0401" to "Débit EGR insuffisant",
        "P0420" to "Rendement catalyseur faible (Bank 1)",
        "P0440" to "Problème système EVAP",

        // Transmission
        "P0700" to "Problème système transmission (voir codes ECU boîte)",
        "P0715" to "Problème capteur vitesse d'entrée (boîte auto)",
        "P0730" to "Problème rapport de vitesse (boîte auto)",

        // Charge / batterie
        "P0562" to "Tension système trop basse (batterie/alternateur)",
        "P0563" to "Tension système trop haute"
    )

    fun describe(code: String): String {
        return descriptions[code]
            ?: "Description non disponible dans la liste de base - recherchez ce code en ligne."
    }
}
