# StartStopp Discord Bot

Ein Discord-Bot mit REST API für die automatische Verwaltung von Docker-Containern über Portainer. Der Bot überwacht Game-Server, startet und stoppt Container automatisch basierend auf Spieleraktivität und bietet sowohl Discord-Commands als auch eine REST API für die Verwaltung.

## 🚀 Features

### Discord Integration
- **Slash Commands** für intuitive Server-Verwaltung
- **Embed-Nachrichten** mit Echtzeit-Status und Spielerzahlen
- **Admin-Berechtigungen** für kritische Operationen
- **Automatische Command-Registrierung** beim Start

### REST API
- **Vollständige CRUD-Operationen** für Server-Verwaltung
- **Swagger UI Dokumentation** unter `/swagger-ui.html`
- **OpenAPI 3.0** konforme API-Spezifikation
- **CORS-Support** für Web-Clients

### Container-Management
- **Portainer Integration** für Docker-Container-Steuerung
- **Automatische Überwachung** (stoppt inaktive Container nach 30 Minuten)
- **Status-Synchronisation** zwischen Bot und Portainer (alle 2 Minuten)
- **Robuste Fehlerbehandlung** mit Retry-Mechanismus

### Datenbank & Persistierung
- **H2 Database** für lokale Datenspeicherung
- **JPA/Hibernate** für objekt-relationales Mapping
- **Automatische Schema-Updates** (`hibernate.ddl-auto=update`)
- **Persistente Container-Zustände** und Spielerstatistiken

## 📋 Discord Commands

| Command | Beschreibung | Berechtigung |
|---------|--------------|--------------|
| `/getserverstatuslist` | Zeigt alle Server mit Status, Spielerzahl und letztem Update | Alle |
| `/getserverstatus <name>` | Detaillierter Status eines bestimmten Servers | Alle |
| `/startserver <name>` | Startet einen Server-Container | Alle |
| `/stopserver <name>` | Stoppt einen Server-Container | Administrator |

## 🔌 REST API Endpoints

### Application Management
```http
GET    /api/applications              # Alle Anwendungen abrufen
GET    /api/applications/{name}       # Spezifische Anwendung abrufen
POST   /api/applications              # Anwendung registrieren/aktualisieren
PUT    /api/applications/{name}       # Spielerzahl aktualisieren
DELETE /api/applications/{name}       # Anwendung löschen
```

### Container Control
```http
POST   /api/applications/{name}/start # Server starten
POST   /api/applications/{name}/stop  # Server stoppen
```

### API Dokumentation
- **Swagger UI**: `http://localhost:8080/swagger-ui.html`
- **OpenAPI JSON**: `http://localhost:8080/api-docs`

## 🛠️ Setup & Installation

### Voraussetzungen
- **Docker & Docker Compose**
- **Portainer** (läuft und ist erreichbar)
- **Discord Bot Token** (siehe Discord Bot erstellen)
- **Java 21** (für lokale Entwicklung)

### 1. Discord Bot erstellen

1. Gehe zu [Discord Developer Portal](https://discord.com/developers/applications)
2. Erstelle eine neue Application → Bot
3. Kopiere den **Bot Token**
4. Aktiviere unter "Privileged Gateway Intents":
   - Message Content Intent (falls benötigt)
5. Lade den Bot auf deinen Server ein mit Berechtigungen:
   - `Send Messages`
   - `Use Slash Commands`
   - `Embed Links`

**Invite Link Generator:**
```
https://discord.com/api/oauth2/authorize?client_id=YOUR_CLIENT_ID&permissions=2147484672&scope=bot%20applications.commands
```

### 2. Portainer Setup

```bash
# Portainer starten (falls noch nicht vorhanden)
docker run -d -p 9000:9000 --name portainer \
  --restart=always \
  -v /var/run/docker.sock:/var/run/docker.sock \
  -v portainer_data:/data \
  portainer/portainer-ce:latest
```

### 3. Bot Deployment

#### Option A: Docker Compose (Empfohlen)

1. **Erstelle `docker-compose.yml`:**
```yaml
version: '3.8'

services:
  startstoppbot:
    build: .
    ports:
      - "8080:8080"
    environment:
      - DISCORD_BOT_TOKEN=${DISCORD_BOT_TOKEN}
      - PORTAINER_URL=http://portainer:9000
      - PORTAINER_USERNAME=${PORTAINER_USERNAME}
      - PORTAINER_PASSWORD=${PORTAINER_PASSWORD}
    volumes:
      - ./DATA/StartStoppBotDc:/DATA/StartStoppBotDc
    networks:
      - portainer_default
    restart: unless-stopped

networks:
  portainer_default:
    external: true
```

2. **Erstelle `.env` Datei:**
```env
DISCORD_BOT_TOKEN=your_discord_bot_token_here
PORTAINER_USERNAME=admin
PORTAINER_PASSWORD=your_portainer_password_here
```

3. **Starte den Bot:**
```bash
docker-compose up -d
```

#### Option B: Standalone Docker

```bash
# Image bauen
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
  --restart=unless-stopped \
  startstoppbot
```

### 4. Erste Anwendung registrieren

**Container ID herausfinden:**
1. Gehe zu Portainer → Containers
2. Klicke auf deinen Container
3. Kopiere die **Container ID** aus der URL oder dem Interface

**Anwendung registrieren:**
```bash
curl -X POST http://localhost:8080/api/applications \
  -H "Content-Type: application/json" \
  -d '{
    "name": "minecraft-server",
    "containerId": "your_container_id_from_portainer",
    "currentPlayers": 0
  }'
```

## ⚙️ Konfiguration

### Environment Variables

| Variable | Beschreibung | Beispiel | Erforderlich |
|----------|--------------|----------|--------------|
| `DISCORD_BOT_TOKEN` | Discord Bot Token | `OTc4M...` | ✅ |
| `PORTAINER_URL` | Portainer Base URL | `http://portainer:9000` | ✅ |
| `PORTAINER_USERNAME` | Portainer Login | `admin` | ✅ |
| `PORTAINER_PASSWORD` | Portainer Passwort | `mySecretPassword` | ✅ |
| `SERVER_PORT` | REST API Port | `8080` | ❌ |

### application.properties Konfiguration

```properties
# Discord Bot
discord.bot.token=${DISCORD_BOT_TOKEN}

# Portainer Integration
portainer.url=${PORTAINER_URL}
portainer.username=${PORTAINER_USERNAME}
portainer.password=${PORTAINER_PASSWORD}

# Database (H2)
spring.datasource.url=jdbc:h2:file:/DATA/StartStoppBotDc/database/startstoppbot
spring.jpa.hibernate.ddl-auto=update

# Logging
logging.file.name=/DATA/StartStoppBotDc/logs/startstoppbot.log
logging.level.com.example.startstoppbot=INFO
```

### Verzeichnisstruktur

```
DATA/StartStoppBotDc/
├── database/
│   ├── startstoppbot.mv.db     # H2 Database
│   └── startstoppbot.trace.db  # H2 Trace Files
└── logs/
    └── startstoppbot.log       # Application Logs
```

## 🔄 Automatische Funktionen

### Container-Überwachung (`SchedulerService`)
- **Inaktivitäts-Check**: Alle 5 Minuten (300.000ms)
   - Container werden nach 30 Minuten ohne Aktivität automatisch gestoppt
   - Status wird auf "offline" gesetzt, Spielerzahl auf 0
- **Status-Synchronisation**: Alle 2 Minuten (120.000ms)
   - Gleicht lokalen Status mit Portainer-Container-Status ab
   - Erkennt manuell gestartete/gestoppte Container

### Portainer Integration
- **JWT-Authentifizierung** mit automatischer Token-Erneuerung
- **Retry-Mechanismus** bei 401 Unauthorized Fehlern
- **Container-Operationen**:
   - `startContainer()` - Startet Docker Container
   - `stopContainer()` - Stoppt Docker Container
   - `isContainerRunning()` - Prüft Container-Status

## 📊 Monitoring & Logs

### Logs anzeigen
```bash
# Docker Container Logs
docker logs startstoppbot -f

# Application Log Files
tail -f DATA/StartStoppBotDc/logs/startstoppbot.log

# Nur Fehler anzeigen
grep ERROR DATA/StartStoppBotDc/logs/startstoppbot.log
```

### Health Checks
```bash
# Spring Boot Actuator
curl http://localhost:8080/actuator/health

# API Verfügbarkeit
curl http://localhost:8080/api/applications
```

### Discord Bot Status
```bash
# Bot Logs für Discord Events
docker logs startstoppbot | grep -i discord
```

## 🐛 Troubleshooting

### Häufige Probleme

**1. Discord Bot antwortet nicht**
```bash
# Prüfe Bot Status
docker logs startstoppbot | grep "Discord Bot gestartet"

# Prüfe Token
echo $DISCORD_BOT_TOKEN | wc -c  # Sollte >50 Zeichen haben
```

**2. Portainer Verbindung fehlgeschlagen**
```bash
# Teste Portainer Erreichbarkeit
curl -X POST http://portainer:9000/api/auth \
  -H "Content-Type: application/json" \
  -d '{"Username":"admin","Password":"your_password"}'

# Prüfe Netzwerk-Verbindung
docker exec startstoppbot ping portainer
```

**3. Container können nicht gestartet/gestoppt werden**
```bash
# Prüfe Container IDs in der Datenbank
curl http://localhost:8080/api/applications

# Teste Container-Operation direkt
curl -X POST http://localhost:8080/api/applications/minecraft-server/start
```

**4. Database Probleme**
```bash
# H2 Console aktivieren (nur für Debug)
# In application.properties: spring.h2.console.enabled=true
# Zugriff: http://localhost:8080/h2-console
# JDBC URL: jdbc:h2:file:/DATA/StartStoppBotDc/database/startstoppbot
```

### Debug-Modus aktivieren

**Environment Variable setzen:**
```bash
-e LOGGING_LEVEL_COM_EXAMPLE_STARTSTOPPBOT=DEBUG
```

**Oder in application.properties:**
```properties
logging.level.com.example.startstoppbot=DEBUG
logging.level.org.springframework.web=DEBUG
```

## 💻 Entwicklung

### Lokale Entwicklung

**Voraussetzungen:**
- Java 21+ (OpenJDK empfohlen)
- Maven 3.8+
- Lokaler Portainer oder Remote-Zugriff

**Setup:**
```bash
# Repository klonen
git clone <repository-url>
cd startstoppbot

# Dependencies installieren
./mvnw dependency:resolve

# Konfiguration erstellen
cp application.properties.example application.properties
# Anpassen der Werte in application.properties

# Anwendung starten
./mvnw spring-boot:run
```

### Build & Packaging

```bash
# Clean Build
./mvnw clean package

# Docker Image erstellen
docker build -t startstoppbot:latest .

# JAR-Datei finden
ls -la target/startstoppbot-*.jar
```

### Datenbank Schema

**Application Entity:**
```sql
CREATE TABLE applications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) UNIQUE NOT NULL,
    container_id VARCHAR(255) NOT NULL,
    current_players INTEGER NOT NULL DEFAULT 0,
    last_update TIMESTAMP NOT NULL,
    is_online BOOLEAN NOT NULL DEFAULT TRUE
);
```

## 🔧 Erweiterte Konfiguration

### Custom Scheduler Intervals

```properties
# Custom Scheduling (in application.properties)
startstoppbot.scheduler.inactive-check-rate=300000  # 5 Minuten
startstoppbot.scheduler.status-sync-rate=120000     # 2 Minuten
startstoppbot.scheduler.offline-threshold=30        # 30 Minuten
```

### CORS Konfiguration

Der Bot ist bereits für Cross-Origin Requests konfiguriert:
```java
// In StartStoppBotApplication.java
registry.addMapping("/**")
        .allowedOrigins("*")
        .allowedMethods("GET", "POST", "PUT", "DELETE");
```

### Custom Portainer Endpoint

Standardmäßig wird Endpoint 1 verwendet. Für andere Endpoints:
```properties
portainer.endpoint.id=2
```

## 📄 API Beispiele

### Vollständige API-Verwendung

**1. Server registrieren:**
```bash
curl -X POST http://localhost:8080/api/applications \
  -H "Content-Type: application/json" \
  -d '{
    "name": "valheim-server",
    "containerId": "f8a9b7c6d5e4f3a2",
    "currentPlayers": 3
  }'
```

**2. Spielerzahl aktualisieren:**
```bash
curl -X PUT http://localhost:8080/api/applications/valheim-server \
  -H "Content-Type: application/json" \
  -d '{"currentPlayers": 8}'
```

**3. Server starten:**
```bash
curl -X POST http://localhost:8080/api/applications/valheim-server/start
```

**4. Alle Server abrufen:**
```bash
curl http://localhost:8080/api/applications | jq '.'
```

## 📈 Version & Updates

**Aktuelle Version:** 1.0.0

### Changelog
- **1.0.0**: Initial Release
   - Discord Slash Commands
   - REST API mit Swagger
   - Portainer Integration
   - Automatische Container-Überwachung
   - H2 Database Persistierung

## 🤝 Support & Kontakt

**Bei Problemen:**
1. Prüfe die Logs: `docker logs startstoppbot`
2. Validiere die Konfiguration
3. Teste API-Endpoints mit curl
4. Öffne ein Issue im Repository

**Nützliche Links:**
- [Discord Developer Portal](https://discord.com/developers/applications)
- [Portainer Documentation](https://docs.portainer.io/)
- [Spring Boot Reference](https://docs.spring.io/spring-boot/docs/current/reference/html/)

---

**Entwickelt mit ❤️ für die Gaming-Community**