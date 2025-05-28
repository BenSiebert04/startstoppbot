# StartStopp Discord Bot

Ein Discord-Bot mit REST API für die automatische Verwaltung von Docker-Containern über Portainer.

## Features

- **Discord Slash Commands** für Server-Management
- **REST API** mit Swagger UI Dokumentation
- **Automatische Container-Überwachung** (stoppt inaktive Container nach 30 Min)
- **Portainer Integration** für Container-Steuerung
- **Persistente Datenspeicherung** mit H2 Database

## Discord Commands

- `/getServerStatusList` - Zeigt alle Server mit Status und Spielerzahl
- `/getServerStatus <name>` - Status eines bestimmten Servers
- `/startServer <name>` - Startet einen Server
- `/stopServer <name>` - Stoppt einen Server (nur Admins)

## REST API Endpoints

- `GET /api/applications` - Alle Anwendungen abrufen
- `GET /api/applications/{name}` - Spezifische Anwendung abrufen
- `POST /api/applications` - Anwendung registrieren/aktualisieren
- `PUT /api/applications/{name}` - Spielerzahl aktualisieren
- `POST /api/applications/{name}/start` - Server starten
- `POST /api/applications/{name}/stop` - Server stoppen

## Setup & Installation

### 1. Discord Bot erstellen

1. Gehe zu https://discord.com/developers/applications
2. Erstelle eine neue Application
3. Gehe zu "Bot" → "Add Bot"
4. Kopiere den Bot Token
5. Aktiviere "Message Content Intent"
6. Lade den Bot auf deinen Server ein mit folgenden Berechtigungen:
   - Send Messages
   - Use Slash Commands
   - Embed Links

### 2. Portainer Setup

Stelle sicher, dass Portainer läuft und zugänglich ist:

```bash
docker run -d -p 9000:9000 --name portainer \
  --restart=always \
  -v /var/run/docker.sock:/var/run/docker.sock \
  -v portainer_data:/data \
  portainer/portainer-ce:latest
```

### 3. Bot Deployment

#### Option A: Mit Docker Compose (Empfohlen)

1. Erstelle eine `.env` Datei:
```env
DISCORD_BOT_TOKEN=your_discord_bot_token_here
PORTAINER_USERNAME=admin
PORTAINER_PASSWORD=your_portainer_password
```

2. Starte den Bot:
```bash
docker-compose up -d
```

#### Option B: Manueller Docker Build

```bash
# Bot bauen
docker build -t startstoppbot .

# Container starten  
docker run -d \
  --name startstoppbot \
  -p 8080:8080 \
  -v $(pwd)/DATA/StartStoppBotDc:/DATA/StartStoppBotDc \
  -e DISCORD_BOT_TOKEN=your_token_here \
  -e PORTAINER_URL=http://portainer:9000 \
  -e PORTAINER_USERNAME=admin \
  -e PORTAINER_PASSWORD=your_password \
  --network=portainer_default \
  startstoppbot
```

### 4. Erste Anwendung registrieren

Nach dem Start kannst du über die REST API eine Anwendung registrieren:

```bash
curl -X POST http://localhost:8080/api/applications \
  -H "Content-Type: application/json" \
  -d '{
    "name": "minecraft-server",
    "containerId": "your_container_id_from_portainer",
    "currentPlayers": 0
  }'
```

## Konfiguration

### Environment Variables

| Variable | Beschreibung | Standard | Erforderlich |
|----------|--------------|----------|--------------|
| `DISCORD_BOT_TOKEN` | Discord Bot Token | - | ✅ |
| `PORTAINER_URL` | Portainer URL | http://localhost:9000 | ✅ |
| `PORTAINER_USERNAME` | Portainer Benutzername | admin | ✅ |
| `PORTAINER_PASSWORD` | Portainer Passwort | - | ✅ |
| `SERVER_PORT` | REST API Port | 8080 | ❌ |

### Verzeichnisstruktur

```
DATA/StartStoppBotDc/
├── database/           # H2 Database Dateien
├── logs/              # Log Dateien
└── startstoppbot.log  # Haupt-Log-Datei
```

## API Dokumentation

Nach dem Start ist die Swagger UI verfügbar unter:
- http://localhost:8080/swagger-ui.html

## Monitoring & Logs

### Logs anzeigen
```bash
# Docker Logs
docker logs startstoppbot -f

# Datei-Logs
tail -f DATA/StartStoppBotDc/logs/startstoppbot.log
```

### Health Check
```bash
curl http://localhost:8080/actuator/health
```

## Automatische Funktionen

- **Container-Überwachung**: Alle 5 Minuten werden inaktive Container überprüft
- **Status-Synchronisation**: Alle 2 Minuten wird der Container-Status mit Portainer abgeglichen
- **Auto-Stop**: Container werden nach 30 Minuten Inaktivität automatisch gestoppt

## Troubleshooting

### Häufige Probleme

1. **Bot antwortet nicht**
   - Prüfe Discord Bot Token
   - Überprüfe Bot-Berechtigungen auf dem Server

2. **Portainer Verbindung fehlgeschlagen**
   - Prüfe Portainer URL und Zugangsdaten
   - Stelle sicher, dass der Bot das Portainer-Netzwerk erreichen kann

3. **Container können nicht gestartet/gestoppt werden**
   - Überprüfe Container-IDs in der Datenbank
   - Prüfe Portainer-Berechtigungen

### Debug-Modus

Setze folgende Environment Variable für detailliertere Logs:
```
LOGGING_LEVEL_COM_EXAMPLE_STARTSTOPPBOT=DEBUG
```

## Entwicklung

### Lokale Entwicklung

1. Java 17+ installieren
2. `application.properties` konfigurieren
3. Bot starten: `./mvnw spring-boot:run`

### Build

```bash
./mvnw clean package
```

## Support

Bei Problemen oder Fragen öffne ein Issue im Repository oder kontaktiere den Administrator.

## Version

Aktuelle Version: 1.0.0
