package com.example.startstoppbot.dto;

import io.swagger.v3.oas.annotations.media.Schema;

// Separate Klasse für Update Request
@Schema(description = "Request für das Aktualisieren der Spieleranzahl")
public class ApplicationUpdateRequest {
    
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
