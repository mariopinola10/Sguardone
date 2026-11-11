# IPTV Lite (Fire TV)

App Android leggera per Fire TV Stick: liste M3U, guida EPG XMLTV e player Media3.
Tutto il traffico dell'app passa da un tunnel **WireGuard Mullvad**, configurato da solo tramite API.

## Come funziona all'avvio
1. Chiede il consenso VPN di sistema (solo la prima volta).
2. Genera una chiave WireGuard e la registra sull'account Mullvad (`api.mullvad.net/wg/`), solo la prima volta.
3. Scarica la lista server, sceglie un server del paese impostato e alza il tunnel, **solo per questa app**.
4. Verifica su `am.i.mullvad.net/connected` che il traffico esca davvero da Mullvad.
5. Solo dopo scarica la lista M3U, mostra i canali e aggiorna l'EPG in background (cache di 12 ore).

**Kill-switch applicativo**: se il tunnel cade, il player si ferma e la UI mostra "Riprova".
All'uscita dall'app il tunnel viene chiuso.

## Compilazione
1. Apri la cartella in **Android Studio** (Ladybug o successivo, JDK 17+).
2. Aspetta la sincronizzazione Gradle.
3. Menu **Build → Build App Bundle(s) / APK(s) → Build APK(s)**.
4. L'APK si trova in `app/build/outputs/apk/debug/app-debug.apk`.

Da riga di comando: `./gradlew assembleDebug`

## Compilazione con GitHub (senza PC)
A ogni push su `main`, GitHub Actions compila l'APK e lo pubblica come release.
Link fisso all'ultima versione:
`https://github.com/<utente>/<repo>/releases/latest/download/IptvLite.apk`

## Installazione su Fire Stick
1. Sul Fire Stick: Impostazioni → La mia Fire TV → Opzioni sviluppatore → **Debug ADB: ON**.
2. Annota l'IP del Fire Stick (Impostazioni → La mia Fire TV → Informazioni → Rete).
3. Dal PC:
   ```
   adb connect 192.168.1.XX:5555
   adb install -r app-debug.apk
   ```
4. Log per il debug: `adb logcat | grep -iE "iptvlite|WireGuard|ExoPlayer|AndroidRuntime"`

**Nota**: i modelli Fire TV con **Vega OS** non eseguono APK. Serve un modello con Fire OS.

## Configurazione
Al primo avvio si aprono le impostazioni:
- **Account Mullvad**: le 16 cifre.
- **Paese**: codice a due lettere (`it`, `ch`, `de`, `nl`…).
- **Lista M3U**: un **link** oppure un **file** (anche `.gz`).
- **Guida EPG XMLTV**: facoltativa. Se vuota, l'app usa `url-tvg` / `x-tvg-url` dall'intestazione della lista.

### Dal telefono (consigliato)
Con le impostazioni aperte, la TV mostra un indirizzo e un PIN, per esempio `http://192.168.1.50:8080 PIN 4821`.
1. Apri l'indirizzo dal browser del telefono, collegato alla stessa Wi-Fi.
2. Inserisci il PIN, l'account Mullvad e il link M3U, oppure scegli il file M3U dal telefono.
3. Premi **Invia alla TV**: l'app salva la configurazione e riparte.

I campi lasciati vuoti mantengono il valore attuale. Il server è attivo solo mentre le impostazioni sono aperte.

### Dal telecomando
Compila i campi e premi **Salva**. Il pulsante **Scegli file M3U** usa il selettore file di sistema, che su molti Fire Stick manca: in quel caso usa il telefono.

## Verifica HTTPS
- **Lista M3U e guida EPG**: solo `https://`. I link HTTP vengono rifiutati nelle impostazioni e ricontrollati prima di ogni download. Viene bloccato anche un redirect HTTPS → HTTP.
- **Stream dei canali**: per default ogni connessione, compresi segmenti HLS e redirect, deve essere HTTPS. In caso contrario il canale viene bloccato con un messaggio.
- All'import la lista indica quanti canali non sono HTTPS.
- L'opzione "Consenti stream dei canali non HTTPS" nelle impostazioni sblocca gli stream HTTP. Restano comunque cifrati verso l'ISP dal tunnel Mullvad, ma Mullvad potrebbe vederne il contenuto.

## Telecomando
- **Lista**: frecce per muoverti, OK per aprire il canale, tasto Menu per le impostazioni.
- **Player**: SU/GIÙ cambia canale, OK mostra le info, INDIETRO torna alla lista.

## Limiti noti
- Mullvad consente al massimo **5 dispositivi** per account. "Rigenera chiave VPN" ne usa uno nuovo: rimuovi quello vecchio dal sito Mullvad.
- La chiave privata WireGuard è salvata nello storage privato dell'app (non in un keystore hardware).
- Il kill-switch è a livello di app: in caso di caduta del tunnel può esserci un breve istante prima che il player si fermi.
- I loghi dei canali non sono mostrati, per tenere l'app leggera.

## Struttura
```
vpn/MullvadApi.kt      registrazione chiave + lista server
vpn/VpnManager.kt      tunnel WireGuard (GoBackend), verifica, stato
data/M3uParser.kt      parser M3U in streaming (tvg-*, group-title, EXTVLCOPT)
data/EpgImporter.kt    XMLTV in streaming -> SQLite (finestra -2h/+48h)
data/Repository.kt     download (gzip auto), cache, query
ui/MainActivity.kt     gruppi | canali | guida
ui/PlayerActivity.kt   Media3 ExoPlayer (HLS/TS), zapping, retry
ui/SettingsActivity.kt configurazione (telecomando, file, telefono)
config/ConfigServer.kt mini server web locale per configurare dal telefono
```
