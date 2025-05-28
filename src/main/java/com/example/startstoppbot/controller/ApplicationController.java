package com.example.startstoppbot.controller;

import com.example.startstoppbot.dto.ApplicationCreateRequest;
import com.example.startstoppbot.dto.ApplicationUpdateRequest;
import com.example.startstoppbot.model.Application;
import com.example.startstoppbot.service.ApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/applications")
@Tag(name = "Application Management", description = "APIs für die Verwaltung von Anwendungen")
public class ApplicationController {

    @Autowired
    private ApplicationService applicationService;

    @GetMapping
    @Operation(summary = "Alle Anwendungen abrufen", description = "Gibt eine Liste aller registrierten Anwendungen zurück")
    @ApiResponse(responseCode = "200", description = "Liste der Anwendungen erfolgreich abgerufen")
    public ResponseEntity<List<Application>> getAllApplications() {
        List<Application> applications = applicationService.getAllApplications();
        return ResponseEntity.ok(applications);
    }

    @GetMapping("/{name}")
    @Operation(summary = "Anwendung nach Name abrufen", description = "Gibt Details einer spezifischen Anwendung zurück")
    @ApiResponse(responseCode = "200", description = "Anwendung gefunden")
    @ApiResponse(responseCode = "404", description = "Anwendung nicht gefunden")
    public ResponseEntity<Application> getApplicationByName(
            @Parameter(description = "Name der Anwendung") @PathVariable String name) {
        Optional<Application> application = applicationService.getApplicationByName(name);
        return application.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Neue Anwendung erstellen", description = "Registriert eine neue Anwendung oder aktualisiert eine bestehende")
    @ApiResponse(responseCode = "201", description = "Anwendung erfolgreich erstellt")
    @ApiResponse(responseCode = "200", description = "Anwendung erfolgreich aktualisiert")
    @ApiResponse(responseCode = "400", description = "Ungültige Anfrage")
    public ResponseEntity<Application> createOrUpdateApplication(@RequestBody ApplicationCreateRequest request) {
        try {
            boolean existed = applicationService.applicationExists(request.getName());
            Application application = applicationService.createOrUpdateApplication(
                    request.getName(),
                    request.getContainerId(),
                    request.getCurrentPlayers()
            );

            if (existed) {
                return ResponseEntity.ok(application);
            } else {
                return ResponseEntity.status(HttpStatus.CREATED).body(application);
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{name}")
    @Operation(summary = "Spieleranzahl aktualisieren", description = "Aktualisiert die Spieleranzahl einer bestehenden Anwendung")
    @ApiResponse(responseCode = "200", description = "Spieleranzahl erfolgreich aktualisiert")
    @ApiResponse(responseCode = "404", description = "Anwendung nicht gefunden")
    @ApiResponse(responseCode = "400", description = "Ungültige Anfrage")
    public ResponseEntity<Application> updatePlayerCount(
            @Parameter(description = "Name der Anwendung") @PathVariable String name,
            @RequestBody ApplicationUpdateRequest request) {
        try {
            Application application = applicationService.updatePlayerCount(name, request.getCurrentPlayers());
            return ResponseEntity.ok(application);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{name}")
    @Operation(summary = "Anwendung löschen", description = "Entfernt eine Anwendung aus der Datenbank und stoppt optional den Container")
    @ApiResponse(responseCode = "200", description = "Anwendung erfolgreich gelöscht")
    @ApiResponse(responseCode = "404", description = "Anwendung nicht gefunden")
    @ApiResponse(responseCode = "500", description = "Fehler beim Löschen der Anwendung")
    public ResponseEntity<String> deleteApplication(
            @Parameter(description = "Name der Anwendung") @PathVariable String name) {
        try {
            if (applicationService.deleteApplication(name)) {
                return ResponseEntity.ok("Anwendung '" + name + "' erfolgreich gelöscht");
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Fehler beim Löschen der Anwendung '" + name + "': " + e.getMessage());
        }
    }

    @PostMapping("/{name}/start")
    @Operation(summary = "Server starten", description = "Startet den Docker-Container der Anwendung")
    @ApiResponse(responseCode = "200", description = "Server erfolgreich gestartet")
    @ApiResponse(responseCode = "404", description = "Anwendung nicht gefunden")
    @ApiResponse(responseCode = "500", description = "Fehler beim Starten des Servers")
    public ResponseEntity<String> startServer(
            @Parameter(description = "Name der Anwendung") @PathVariable String name) {
        if (applicationService.startServer(name)) {
            return ResponseEntity.ok("Server '" + name + "' erfolgreich gestartet");
        } else {
            if (applicationService.applicationExists(name)) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Fehler beim Starten des Servers '" + name + "'");
            } else {
                return ResponseEntity.notFound().build();
            }
        }
    }

    @PostMapping("/{name}/stop")
    @Operation(summary = "Server stoppen", description = "Stoppt den Docker-Container der Anwendung")
    @ApiResponse(responseCode = "200", description = "Server erfolgreich gestoppt")
    @ApiResponse(responseCode = "404", description = "Anwendung nicht gefunden")
    @ApiResponse(responseCode = "500", description = "Fehler beim Stoppen des Servers")
    public ResponseEntity<String> stopServer(
            @Parameter(description = "Name der Anwendung") @PathVariable String name) {
        if (applicationService.stopServer(name)) {
            return ResponseEntity.ok("Server '" + name + "' erfolgreich gestoppt");
        } else {
            if (applicationService.applicationExists(name)) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Fehler beim Stoppen des Servers '" + name + "'");
            } else {
                return ResponseEntity.notFound().build();
            }
        }
    }
}