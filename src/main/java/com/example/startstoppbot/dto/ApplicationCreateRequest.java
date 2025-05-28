package com.example.startstoppbot.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request für das Erstellen einer neuen Anwendung")
public class ApplicationCreateRequest {
    
    @Schema(description = "Name der Anwendung", required = true, example = "minecraft-server")
    private String name;
    
    @Schema(description = "Docker Container ID aus Portainer", required = true, example = "abc123def456")
    private String containerId;
    
    @Schema(description = "Aktuelle Anzahl der Spieler", required = true, example = "5")
    private Integer currentPlayers;

    // Konstruktoren
    public ApplicationCreateRequest() {}

    public ApplicationCreateRequest(String name, String containerId, Integer currentPlayers) {
        this.name = name;
        this.containerId = containerId;
        this.currentPlayers = currentPlayers;
    }

    // Getter und Setter
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getContainerId() {
        return containerId;
    }

    public void setContainerId(String containerId) {
        this.containerId = containerId;
    }

    public Integer getCurrentPlayers() {
        return currentPlayers;
    }

    public void setCurrentPlayers(Integer currentPlayers) {
        this.currentPlayers = currentPlayers;
    }
}

// Separate Klasse für Update Request
@Schema(description = "Request für das Aktualisieren der Spieleranzahl")
class ApplicationUpdateRequest {
    
    @Schema(description = "Aktuelle Anzahl der Spieler", required = true, example = "8")
    private Integer currentPlayers;

    // Konstruktoren
    public ApplicationUpdateRequest() {}

    public ApplicationUpdateRequest(Integer currentPlayers) {
        this.currentPlayers = currentPlayers;
    }

    // Getter und Setter
    public Integer getCurrentPlayers() {
        return currentPlayers;
    }

    public void setCurrentPlayers(Integer currentPlayers) {
        this.currentPlayers = currentPlayers;
    }
}