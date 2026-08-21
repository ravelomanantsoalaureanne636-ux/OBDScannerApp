# OBD Scanner - Application Android (Kotlin)

Application Android basique pour lire les codes DTC et afficher des données en temps réel via un scanner ELM327, en Bluetooth.

## Ce que l'application fait

- Sélection d'un scanner ELM327 déjà appairé en Bluetooth
- Connexion via le profil série Bluetooth (SPP)
- Lecture des codes DTC (Mode 03)
- Effacement des codes DTC (Mode 04)
- Affichage en temps réel : RPM, température moteur, vitesse

## Les limites (important)

- Protocole **OBD-II standard uniquement** (moteur) - pas ABS/airbag/transmission en détail
- **Pas de contrôle bidirectionnel** (activer un injecteur, un actionneur, etc.)
- **Pas de codage/programmation ECU**
- Ces fonctions avancées restent réservées aux scanners professionnels (Launch X431, Autel, etc.) qui utilisent des protocoles propriétaires par constructeur

## Comment construire l'APK

### Avec Android Studio (recommandé)

1. Ouvrez Android Studio
2. "Open" > sélectionnez le dossier `OBDScannerApp`
3. Laissez Gradle synchroniser (télécharge les dépendances)
4. Menu Build > Build Bundle(s) / APK(s) > Build APK(s)
5. L'APK se trouve dans `app/build/outputs/apk/debug/app-debug.apk`

### Avec GitHub Codespaces / ligne de commande

1. Ouvrez le projet dans un Codespace (ou terminal local avec le SDK Android installé)
2. Lancez :
   ```bash
   ./gradlew assembleDebug
   ```
3. Récupérez l'APK dans `app/build/outputs/apk/debug/`
4. Téléchargez-le puis installez-le sur votre téléphone :
   ```bash
   adb install -r app-debug.apk
   ```
   (Codespaces étant distant, il n'y a pas d'accès USB direct — il faut télécharger l'APK et l'installer depuis votre PC local)

## Utilisation sur le téléphone

1. Activez le Bluetooth du téléphone
2. Appairez le scanner ELM327 depuis les paramètres Bluetooth (code habituel : 1234 ou 0000)
3. Ouvrez l'application, suivez les étapes numérotées à l'écran (1 à 5)
4. Autorisez les permissions Bluetooth demandées au premier lancement

## Structure du projet

```
OBDScannerApp/
├── build.gradle                  (config projet)
├── settings.gradle
└── app/
    ├── build.gradle               (dépendances de l'app)
    └── src/main/
        ├── AndroidManifest.xml    (permissions Bluetooth)
        ├── java/com/example/obdscanner/
        │   ├── MainActivity.kt    (interface utilisateur + logique)
        │   ├── ELM327.kt          (communication Bluetooth avec le scanner)
        │   └── DtcCodes.kt        (dictionnaire des codes DTC courants)
        └── res/
            ├── layout/activity_main.xml
            └── values/strings.xml, styles.xml
```

## Extensions possibles

- Ajouter plus de codes DTC dans `DtcCodes.kt`
- Ajouter d'autres PID (pression carburant, débit MAF, etc.)
- Graphiques pour visualiser les données en temps réel
- Historique des scans enregistré localement (Room database)
