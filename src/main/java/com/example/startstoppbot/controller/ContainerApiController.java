package com.example.startstoppbot.controller;

import com.example.startstoppbot.model.ContainerInfo;
import com.example.startstoppbot.repository.ContainerInfoRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/containers")
@Tag(name = "Container Management", description = "API für die Verwaltung von Docker-Containern")
public class ContainerApiController {

    private final ContainerInfoRepository containerInfoRepository;

    public ContainerApiController(ContainerInfoRepository containerInfoRepository) {
        this.containerInfoRepository = containerInfoRepository;
    }

    @GetMapping
    @Operation(summary = "Alle Container abrufen", description = "Gibt eine Liste aller Container zurück")
    public ResponseEntity<List<ContainerInfo>> getAllContainers() {
        List<ContainerInfo> containers = containerInfoRepository.findAll();
        return ResponseEntity.ok(containers);
    }

    @GetMapping("/discord-enabled")
    @Operation(summary = "Discord-fähige Container abrufen", description = "Gibt alle Container zurück, die für Discord freigegeben sind")
    public ResponseEntity<List<ContainerInfo>> getDiscordEnabledContainers() {
        List<ContainerInfo> containers = containerInfoRepository.findByDiscordEnabledTrue();
        return ResponseEntity.ok(containers);
    }

    @GetMapping("/{containerName}")
    @Operation(summary = "Container nach Namen abrufen", description = "Gibt einen spezifischen Container anhand des Namens zurück")
    public ResponseEntity<ContainerInfo> getContainerByName(
            @Parameter(description = "Container-Name") @PathVariable String containerName) {
        Optional<ContainerInfo> container = containerInfoRepository.findById(containerName);
        return container.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{containerName}/discord-enable")
    @Operation(summary = "Discord-Berechtigung aktivieren", description = "Aktiviert die Discord-Steuerung für einen Container")
    @Transactional
    public ResponseEntity<String> enableDiscordControl(
            @Parameter(description = "Container-Name") @PathVariable String containerName) {
        int updated = containerInfoRepository.updateDiscordEnabledByName(containerName, true);
        if (updated > 0) {
            return ResponseEntity.ok("Discord-Steuerung für Container '" + containerName + "' aktiviert");
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{containerName}/discord-disable")
    @Operation(summary = "Discord-Berechtigung deaktivieren", description = "Deaktiviert die Discord-Steuerung für einen Container")
    @Transactional
    public ResponseEntity<String> disableDiscordControl(
            @Parameter(description = "Container-Name") @PathVariable String containerName) {
        int updated = containerInfoRepository.updateDiscordEnabledByName(containerName, false);
        if (updated > 0) {
            return ResponseEntity.ok("Discord-Steuerung für Container '" + containerName + "' deaktiviert");
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{containerName}/discord-toggle")
    @Operation(summary = "Discord-Berechtigung umschalten", description = "Schaltet die Discord-Steuerung für einen Container um")
    @Transactional
    public ResponseEntity<ContainerDiscordStatus> toggleDiscordControl(
            @Parameter(description = "Container-Name") @PathVariable String containerName) {
        Optional<ContainerInfo> containerOpt = containerInfoRepository.findById(containerName);
        if (containerOpt.isPresent()) {
            ContainerInfo container = containerOpt.get();
            boolean newStatus = !container.getDiscordEnabled();
            container.setDiscordEnabled(newStatus);
            containerInfoRepository.save(container);

            return ResponseEntity.ok(new ContainerDiscordStatus(container.getContainerId(), containerName, newStatus));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // ===== NEUE SPIELERZAHL-APIs =====

    @PutMapping("/{containerName}/players")
    @Operation(summary = "Spielerzahl aktualisieren", description = "Aktualisiert die aktuelle Spielerzahl für einen Container")
    @Transactional
    public ResponseEntity<PlayerCountResponse> updatePlayerCount(
            @Parameter(description = "Container-Name") @PathVariable String containerName,
            @RequestBody PlayerCountRequest request) {

        int updated = containerInfoRepository.updatePlayerCountByName(
                containerName,
                request.currentPlayers,
                LocalDateTime.now()
        );

        if (updated > 0) {
            Optional<ContainerInfo> container = containerInfoRepository.findById(containerName);
            if (container.isPresent()) {
                ContainerInfo containerInfo = container.get();
                return ResponseEntity.ok(new PlayerCountResponse(
                        containerInfo.getName(),
                        containerInfo.getCurrentPlayers(),
                        containerInfo.getMaxPlayers(),
                        containerInfo.getLastPlayerUpdate()
                ));
            }
        }

        return ResponseEntity.notFound().build();
    }

    @PutMapping("/{containerName}/players-with-max")
    @Operation(summary = "Spielerzahl mit Maximum aktualisieren", description = "Aktualisiert die aktuelle und maximale Spielerzahl für einen Container")
    @Transactional
    public ResponseEntity<PlayerCountResponse> updatePlayerCountWithMax(
            @Parameter(description = "Container-Name") @PathVariable String containerName,
            @RequestBody PlayerCountWithMaxRequest request) {

        int updated = containerInfoRepository.updatePlayerCountWithMaxByName(
                containerName,
                request.currentPlayers,
                request.maxPlayers,
                LocalDateTime.now()
        );

        if (updated > 0) {
            Optional<ContainerInfo> container = containerInfoRepository.findById(containerName);
            if (container.isPresent()) {
                ContainerInfo containerInfo = container.get();
                return ResponseEntity.ok(new PlayerCountResponse(
                        containerInfo.getName(),
                        containerInfo.getCurrentPlayers(),
                        containerInfo.getMaxPlayers(),
                        containerInfo.getLastPlayerUpdate()
                ));
            }
        }

        return ResponseEntity.notFound().build();
    }

    @GetMapping("/{containerName}/players")
    @Operation(summary = "Spielerzahl abrufen", description = "Gibt die aktuelle Spielerzahl für einen Container zurück")
    public ResponseEntity<PlayerCountResponse> getPlayerCount(
            @Parameter(description = "Container-Name") @PathVariable String containerName) {

        Optional<ContainerInfo> container = containerInfoRepository.findById(containerName);
        if (container.isPresent()) {
            ContainerInfo containerInfo = container.get();
            return ResponseEntity.ok(new PlayerCountResponse(
                    containerInfo.getName(),
                    containerInfo.getCurrentPlayers(),
                    containerInfo.getMaxPlayers(),
                    containerInfo.getLastPlayerUpdate()
            ));
        }

        return ResponseEntity.notFound().build();
    }

    @GetMapping("/active-players")
    @Operation(summary = "Container mit aktiven Spielern", description = "Gibt alle Container mit mindestens einem aktiven Spieler zurück")
    public ResponseEntity<List<ContainerInfo>> getContainersWithActivePlayers() {
        List<ContainerInfo> containers = containerInfoRepository.findByCurrentPlayersGreaterThan(0);
        return ResponseEntity.ok(containers);
    }

    @GetMapping("/players/range")
    @Operation(summary = "Container nach Spielerzahl-Bereich", description = "Gibt Container zurück, deren Spielerzahl im angegebenen Bereich liegt")
    public ResponseEntity<List<ContainerInfo>> getContainersByPlayerRange(
            @Parameter(description = "Minimale Spielerzahl") @RequestParam(defaultValue = "0") Integer minPlayers,
            @Parameter(description = "Maximale Spielerzahl") @RequestParam(defaultValue = "999") Integer maxPlayers) {

        List<ContainerInfo> containers = containerInfoRepository.findByPlayerCountBetween(minPlayers, maxPlayers);
        return ResponseEntity.ok(containers);
    }

    // ===== DTOs =====

    public static class ContainerDiscordStatus {
        public String containerId;
        public String containerName;
        public boolean discordEnabled;

        public ContainerDiscordStatus(String containerId, String containerName, boolean discordEnabled) {
            this.containerId = containerId;
            this.containerName = containerName;
            this.discordEnabled = discordEnabled;
        }
    }

    public static class PlayerCountRequest {
        public Integer currentPlayers;

        public PlayerCountRequest() {}

        public PlayerCountRequest(Integer currentPlayers) {
            this.currentPlayers = currentPlayers;
        }
    }

    public static class PlayerCountWithMaxRequest {
        public Integer currentPlayers;
        public Integer maxPlayers;

        public PlayerCountWithMaxRequest() {}

        public PlayerCountWithMaxRequest(Integer currentPlayers, Integer maxPlayers) {
            this.currentPlayers = currentPlayers;
            this.maxPlayers = maxPlayers;
        }
    }

    public static class PlayerCountResponse {
        public String containerName;
        public Integer currentPlayers;
        public Integer maxPlayers;
        public LocalDateTime lastUpdate;

        public PlayerCountResponse(String containerName, Integer currentPlayers, Integer maxPlayers, LocalDateTime lastUpdate) {
            this.containerName = containerName;
            this.currentPlayers = currentPlayers;
            this.maxPlayers = maxPlayers;
            this.lastUpdate = lastUpdate;
        }
    }
}