# StartStopp Discord Bot

Ein Discord Bot zur automatisierten Verwaltung von Docker-Containern über Portainer. Der Bot ermöglicht es Discord-Nutzern, Container zu starten, zu stoppen und deren Status abzufragen. Zusätzlich bietet er eine REST-API für erweiterte Funktionen und automatisches Stoppen inaktiver Container.

## Features

### Discord Slash Commands
- `/getserverstatuslist` - Zeigt alle für Discord freigegebene Container mit Status und Spielerzahl
- `/getserverstatus <name>` - Detaillierter Status eines bestimmten Containers
- `/startserver <name>` - Startet einen Container
- `/stopserver <name>` - Stoppt einen Container (Admin-berechtigt)

### REST API
- Container-Verwaltung über HTTP-Endpunkte
- Discord-Berechtigungen pro Container aktivieren/deaktivieren
- Spielerzahl-Tracking für Gaming-Server
- Swagger-UI für API-Dokumentation unter `/swagger-ui.html`

### Automatisierung
- **Auto-Stop**: Stoppt Container automatisch bei Inaktivität
- **Spielerzahl-Tracking**: Überwacht aktive Spieler auf Gaming-Servern
- **Scheduler**: Regelmäßige Updates der Container-Informationen
- **Cleanup**: Automatische Bereinigung alter Daten

### Sicherheit
- Container müssen explizit für Discord freigegeben werden
- Getrennte Berechtigungen für Start/Stop-Operationen
- Admin-Kontrolle über kritische Funktionen

## Voraussetzungen

- Docker & Docker Compose
- Portainer-Installation mit API-Zugang
- Discord Bot Token
- Java 21 Runtime (im Container enthalten)

## Installation

### 1. Repository klonen
```bash
git clone <repository-url>
cd startstoppbot
```

### 2. Umgebungsvariablen konfigurieren
Erstelle eine `.env`-Datei im Projektverzeichnis:

```env
# Discord Bot Configuration
DISCORD_BOT_TOKEN=your_discord_bot_token_here

# Portainer Configuration
PORTAINER_URL=http://your-portainer-url:9000
PORTAINER_USERNAME=your_portainer_username
PORTAINER_PASSWORD=your_portainer_password
```

### 3. Discord Bot erstellen
1. Gehe zur [Discord Developer Portal](https://discord.com/developers/applications)
2. Erstelle eine neue Application
3. Erstelle einen Bot und kopiere den Token
4. Aktiviere unter "Bot" die erforderlichen Intents:
    - Slash Commands verwenden
5. Lade den Bot auf deinen Discord-Server ein mit den Berechtigungen:
    - `applications.commands`
    - `bot`

### 4. Verzeichnisstruktur erstellen
```bash
sudo mkdir -p /DATA/StartStoppBotDc/{database,logs}
sudo chown -R $(id -u):$(id -g) /DATA/StartStoppBotDc
```

### 5. Anwendung bauen
```bash
mvn clean package -DskipTests
```

### 6. JAR-Datei bereitstellen
```bash
cp target/startstoppbot-1.0.0.jar /DATA/StartStoppBotDc/
```

### 7. Container starten
```bash
docker-compose up -d
```

## Konfiguration

### Container für Discord freigeben
Standardmäßig sind Container **nicht** für Discord-Steuerung freigegeben. Um Container zu aktivieren:

#### Via REST API:
```bash
# Container für Discord aktivieren
curl -X PUT http://localhost:12346/api/containers/container-name/discord-enable

# Container für Discord deaktivieren  
curl -X PUT http://localhost:12346/api/containers/container-name/discord-disable

# Status umschalten
curl -X POST http://localhost:12346/api/containers/container-name/discord-toggle
```

#### Via Swagger UI:
Öffne `http://localhost:12346/swagger-ui.html` für eine grafische API-Oberfläche.

### Auto-Stop Konfiguration
In `application.properties`:
```properties
# Auto-Stop aktivieren/deaktivieren
scheduler.auto-stop.enabled=true

# Inaktivitäts-Schwelle in Minuten
scheduler.auto-stop.inactivity-minutes=30

# Spielerzahl-basiertes Stoppen
scheduler.auto-stop.check-players=true
```

## Verwendung

### Discord Commands
1. Verwende `/getserverstatuslist` um alle verfügbaren Container zu sehen
2. Starte Container mit `/startserver <container-name>`
3. Überprüfe Status mit `/getserverstatus <container-name>`
4. Stoppe Container mit `/stopserver <container-name>` (wenn berechtigt)

### Spielerzahl-Tracking
Für Gaming-Server kann die Spielerzahl über die API aktualisiert werden:

```bash
# Aktuelle Spielerzahl setzen
curl -X PUT http://localhost:12346/api/containers/minecraft-server/players \
  -H "Content-Type: application/json" \
  -d '{"currentPlayers": 5}'

# Mit maximaler Spielerzahl
curl -X PUT http://localhost:12346/api/containers/minecraft-server/players-with-max \
  -H "Content-Type: application/json" \
  -d '{"currentPlayers": 5, "maxPlayers": 20}'
```

### Monitoring
Der Bot bietet verschiedene Monitoring-Endpunkte:

- **Health Check**: `http://localhost:12346/actuator/health`
- **Container-Status**: `http://localhost:12346/api/containers`
- **Aktive Spieler**: `http://localhost:12346/api/containers/active-players`
- **Logs**: In `/DATA/StartStoppBotDc/logs/startstoppbot.log`

## Architektur

### Komponenten
- **StartStoppBotApplication**: Hauptklasse mit Discord Bot Setup
- **DiscordSlashCommands**: Handler für Discord Slash Commands
- **PortainerService**: Integration mit Portainer API
- **SchedulerService**: Automatisierung und Cleanup-Tasks
- **ContainerApiController**: REST API Endpunkte
- **ContainerInfo**: Entity für Container-Informationen mit H2-Datenbank

### Datenbank
- **H2-Datenbank** zur persistenten Speicherung von Container-Informationen
- **Automatische Schema-Updates** bei Anwendungsstart
- **Backup** in `/DATA/StartStoppBotDc/database/`

### Scheduler Tasks
- **Container-Update**: Alle 4 Minuten
- **Auto-Stop Check**: Alle 5 Minuten
- **Status-Report**: Alle 30 Minuten
- **Cleanup**: Täglich

## Logs und Debugging

### Log-Dateien
```bash
# Live-Logs anzeigen
docker logs -f startstoppbot

# Log-Datei auf dem Host
tail -f /DATA/StartStoppBotDc/logs/startstoppbot.log
```

### Häufige Probleme

**Bot antwortet nicht auf Commands:**
- Überprüfe Discord Bot Token
- Stelle sicher dass Bot auf Server eingeladen ist
- Prüfe Bot-Berechtigungen

**Container können nicht gestartet werden:**
- Überprüfe Portainer-Verbindung
- Stelle sicher dass Container für Discord aktiviert ist
- Prüfe Portainer-Benutzerberechtigungen

**API nicht erreichbar:**
- Überprüfe Port-Mapping (12346)
- Stelle sicher dass Container läuft
- Prüfe Firewall-Einstellungen

## Development

### Lokale Entwicklung
```bash
# Dependencies installieren
mvn clean install

# Anwendung lokal starten (mit .env-Datei)
mvn spring-boot:run

# Tests ausführen
mvn test
```

### API-Dokumentation
Die vollständige API-Dokumentation ist verfügbar unter:
- **Swagger UI**: `http://localhost:12346/swagger-ui.html`
- **OpenAPI JSON**: `http://localhost:12346/api-docs`

## Sicherheitshinweise

- Verwende starke Passwörter für Portainer
- Beschränke Discord Bot-Berechtigungen auf erforderliches Minimum
- Überwache Logs regelmäßig auf ungewöhnliche Aktivitäten
- Halte Container-Images aktuell
- Sichere die `.env`-Datei vor unbefugtem Zugriff

## License

MIT License - Siehe [LICENSE](LICENSE) für Details.

## Support

Bei Problemen oder Fragen:
1. Überprüfe die Logs in `/DATA/StartStoppBotDc/logs/`
2. Konsultiere die API-Dokumentation unter `/swagger-ui.html`
3. Erstelle ein Issue im Repository

---

**Version**: 1.0.0  
