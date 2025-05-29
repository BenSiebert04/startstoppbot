package com.example.startstoppbot.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
public class ContainerInfo {
    @Id
    private String name; // Name als Primärschlüssel (bleibt bei Stack-Neustart gleich)
    private String containerId; // Container-ID (ändert sich bei Stack-Neustart)
    private String status;
    private Integer endpointId; // Endpoint-ID aus Portainer

    @Column(name = "discord_enabled", nullable = false)
    private Boolean discordEnabled = false; // Standard: Discord-Steuerung deaktiviert

    @Column(name = "current_players")
    private Integer currentPlayers = 0; // Aktuelle Spielerzahl

    @Column(name = "max_players")
    private Integer maxPlayers; // Maximale Spielerzahl (optional)

    @Column(name = "last_player_update")
    private LocalDateTime lastPlayerUpdate; // Zeitstempel der letzten Spielerzahl-Aktualisierung
}