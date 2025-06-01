package com.example.startstoppbot.service;

import com.example.startstoppbot.model.ContainerInfo;
import com.example.startstoppbot.repository.ContainerInfoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SchedulerService {

    @Autowired
    private DockerService dockerService;

    @Autowired
    private ContainerInfoRepository containerInfoRepository;

    @Value("${scheduler.auto-stop.enabled:true}")
    private boolean autoStopEnabled;

    @Value("${scheduler.auto-stop.inactivity-minutes:30}")
    private int inactivityMinutes;

    @Value("${scheduler.auto-stop.check-players:true}")
    private boolean checkPlayersForAutoStop;

    /**
     * Überprüft alle 5 Minuten, ob Container gestoppt werden müssen
     * Container werden gestoppt, wenn sie eine bestimmte Zeit nicht mehr aktiv waren
     */
    @Scheduled(fixedRate = 300000) // 5 Minuten in Millisekunden
    public void checkAndStopInactiveContainers() {
        if (!autoStopEnabled) {
            return;
        }

        try {
            LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(inactivityMinutes);
            List<ContainerInfo> runningContainers = containerInfoRepository.findByDiscordEnabledTrue()
                    .stream()
                    .filter(container -> "running".equalsIgnoreCase(container.getStatus()))
                    .toList();

            for (ContainerInfo container : runningContainers) {
                boolean shouldStop = false;
                String reason = "";

                if (checkPlayersForAutoStop && container.getCurrentPlayers() != null) {
                    // Stoppe Container wenn keine Spieler online sind und letzte Aktualisierung alt ist
                    if (container.getCurrentPlayers() == 0 &&
                            container.getLastPlayerUpdate() != null &&
                            container.getLastPlayerUpdate().isBefore(cutoffTime)) {
                        shouldStop = true;
                        reason = "Keine Spieler seit " + inactivityMinutes + " Minuten";
                    }
                } else {
                    // Fallback: Stoppe Container nach allgemeiner Inaktivitätszeit
                    // (Dies kann erweitert werden um andere Inaktivitätskriterien)
                    if (container.getLastPlayerUpdate() != null &&
                            container.getLastPlayerUpdate().isBefore(cutoffTime)) {
                        shouldStop = true;
                        reason = "Inaktiv seit " + inactivityMinutes + " Minuten";
                    }
                }

                if (shouldStop) {
                    try {
                        System.out.println("Auto-Stopp für Container: " + container.getName() + " - Grund: " + reason);
                        dockerService.stoppeContainer(container.getName());

                        // Optional: Spielerzahl auf 0 setzen nach dem Stoppen
                        containerInfoRepository.updatePlayerCountByName(
                                container.getName(), 0, LocalDateTime.now());

                    } catch (Exception e) {
                        System.err.println("Fehler beim automatischen Stoppen von Container " +
                                container.getName() + ": " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Fehler beim Überprüfen inaktiver Container: " + e.getMessage());
        }
    }

    /**
     * Aktualisiert alle 4 Minuten die Datenbank mit aktuellen Container-Informationen
     * und entfernt nicht mehr existierende Container
     */
    @Scheduled(fixedRate = 240000) // 4 Minuten in Millisekunden
    public void updateDB() {
        try {
            System.out.println("Aktualisiere Container-Datenbank...");

            // Hole alle Container aus der Datenbank vor der Aktualisierung
            List<ContainerInfo> dbContainersBefore = containerInfoRepository.findAll();
            Set<String> dbContainerNamesBefore = dbContainersBefore.stream()
                    .map(ContainerInfo::getName)
                    .collect(Collectors.toSet());

            // Aktualisiere die Datenbank mit aktuellen Docker-Containern
            dockerService.aktualisiereDB();

            // Hole alle Container aus der Datenbank nach der Aktualisierung
            List<ContainerInfo> dbContainersAfter = containerInfoRepository.findAll();
            Set<String> dbContainerNamesAfter = dbContainersAfter.stream()
                    .map(ContainerInfo::getName)
                    .collect(Collectors.toSet());

            // Finde Container, die in der DB waren, aber nicht mehr existieren
            Set<String> removedContainers = dbContainerNamesBefore.stream()
                    .filter(name -> !dbContainerNamesAfter.contains(name))
                    .collect(Collectors.toSet());

            // Entferne nicht mehr existierende Container aus der Datenbank
            if (!removedContainers.isEmpty()) {
                System.out.println("Entferne nicht mehr existierende Container: " + removedContainers);
                for (String containerName : removedContainers) {
                    try {
                        containerInfoRepository.deleteByName(containerName);
                        System.out.println("Container '" + containerName + "' aus Datenbank entfernt");
                    } catch (Exception e) {
                        System.err.println("Fehler beim Entfernen von Container '" + containerName + "': " + e.getMessage());
                    }
                }
            }

            System.out.println("Datenbank-Aktualisierung abgeschlossen.");

        } catch (Exception e) {
            System.err.println("Fehler beim Aktualisieren der Datenbank: " + e.getMessage());
        }
    }

    /**
     * Manuelle Methode zum Bereinigen nicht existierender Container
     */
    public void cleanupNonExistentContainers() {
        try {
            System.out.println("Starte manuelle Bereinigung nicht existierender Container...");

            List<ContainerInfo> dbContainers = containerInfoRepository.findAll();

            // Hole aktuelle Docker-Container-Namen
            Set<String> existingDockerContainers = dockerService.getAllContainerNames();

            int removedCount = 0;
            for (ContainerInfo container : dbContainers) {
                if (!existingDockerContainers.contains(container.getName())) {
                    try {
                        containerInfoRepository.deleteByName(container.getName());
                        System.out.println("Nicht existierender Container entfernt: " + container.getName());
                        removedCount++;
                    } catch (Exception e) {
                        System.err.println("Fehler beim Entfernen von Container '" + container.getName() + "': " + e.getMessage());
                    }
                }
            }

            if (removedCount > 0) {
                System.out.println("Bereinigung abgeschlossen. " + removedCount + " Container entfernt.");
            } else {
                System.out.println("Keine nicht existierenden Container gefunden.");
            }

        } catch (Exception e) {
            System.err.println("Fehler beim Bereinigen nicht existierender Container: " + e.getMessage());
        }
    }

    /**
     * Bereinigt alte Spieler-Aktualisierungsdaten alle 24 Stunden
     * Entfernt Einträge, die älter als 7 Tage sind
     */
    @Scheduled(fixedRate = 86400000) // 24 Stunden in Millisekunden
    public void cleanupOldPlayerData() {
        try {
            LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
            List<ContainerInfo> containers = containerInfoRepository.findAll();

            int cleanedCount = 0;
            for (ContainerInfo container : containers) {
                if (container.getLastPlayerUpdate() != null &&
                        container.getLastPlayerUpdate().isBefore(sevenDaysAgo)) {

                    // Setze Spielerzahl auf 0 und entferne alte Zeitstempel
                    containerInfoRepository.updatePlayerCountByName(
                            container.getName(), 0, null);
                    cleanedCount++;
                }
            }

            if (cleanedCount > 0) {
                System.out.println("Bereinigung abgeschlossen. " + cleanedCount +
                        " Container-Spielerdaten zurückgesetzt.");
            }
        } catch (Exception e) {
            System.err.println("Fehler beim Bereinigen alter Spielerdaten: " + e.getMessage());
        }
    }

    /**
     * Status-Report alle 30 Minuten für Debugging/Monitoring
     */
    @Scheduled(fixedRate = 1800000) // 30 Minuten in Millisekunden
    public void logSystemStatus() {
        try {
            List<ContainerInfo> allContainers = containerInfoRepository.findAll();
            List<ContainerInfo> discordEnabled = containerInfoRepository.findByDiscordEnabledTrue();
            List<ContainerInfo> withActivePlayers = containerInfoRepository.findByCurrentPlayersGreaterThan(0);

            long runningContainers = allContainers.stream()
                    .filter(c -> "running".equalsIgnoreCase(c.getStatus()))
                    .count();

            System.out.println("=== System Status ===");
            System.out.println("Gesamt Container: " + allContainers.size());
            System.out.println("Discord-aktiviert: " + discordEnabled.size());
            System.out.println("Laufende Container: " + runningContainers);
            System.out.println("Container mit Spielern: " + withActivePlayers.size());
            System.out.println("Auto-Stopp aktiviert: " + autoStopEnabled);
            System.out.println("Inaktivitäts-Schwelle: " + inactivityMinutes + " Minuten");
            System.out.println("==================");

        } catch (Exception e) {
            System.err.println("Fehler beim Erstellen des Status-Reports: " + e.getMessage());
        }
    }

    /**
     * Manuelle Methode zum Aktivieren/Deaktivieren des Auto-Stop-Features
     */
    public void setAutoStopEnabled(boolean enabled) {
        this.autoStopEnabled = enabled;
        System.out.println("Auto-Stopp " + (enabled ? "aktiviert" : "deaktiviert"));
    }

    /**
     * Manuelle Methode zum Ändern der Inaktivitäts-Schwelle
     */
    public void setInactivityMinutes(int minutes) {
        this.inactivityMinutes = minutes;
        System.out.println("Inaktivitäts-Schwelle auf " + minutes + " Minuten gesetzt");
    }

    /**
     * Gibt den aktuellen Status der Scheduler-Konfiguration zurück
     */
    public String getSchedulerStatus() {
        return String.format(
                "Auto-Stopp: %s | Inaktivitäts-Schwelle: %d Min | Spieler-Check: %s",
                autoStopEnabled ? "AN" : "AUS",
                inactivityMinutes,
                checkPlayersForAutoStop ? "AN" : "AUS"
        );
    }
}