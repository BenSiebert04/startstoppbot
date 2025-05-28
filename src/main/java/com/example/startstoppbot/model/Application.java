package com.example.startstoppbot.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "applications")
public class Application {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String name;
    
    @Column(nullable = false)
    private String containerId;
    
    @Column(nullable = false)
    private Integer currentPlayers;
    
    @Column(nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastUpdate;
    
    @Column(nullable = false)
    private Boolean isOnline;

    // Konstruktoren
    public Application() {
        this.lastUpdate = LocalDateTime.now();
        this.isOnline = true;
        this.currentPlayers = 0;
    }

    public Application(String name, String containerId, Integer currentPlayers) {
        this.name = name;
        this.containerId = containerId;
        this.currentPlayers = currentPlayers;
        this.lastUpdate = LocalDateTime.now();
        this.isOnline = true;
    }

    // Getter und Setter
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public LocalDateTime getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(LocalDateTime lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public Boolean getIsOnline() {
        return isOnline;
    }

    public void setIsOnline(Boolean isOnline) {
        this.isOnline = isOnline;
    }

    public void updateLastSeen() {
        this.lastUpdate = LocalDateTime.now();
        this.isOnline = true;
    }

    @Override
    public String toString() {
        return "Application{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", containerId='" + containerId + '\'' +
                ", currentPlayers=" + currentPlayers +
                ", lastUpdate=" + lastUpdate +
                ", isOnline=" + isOnline +
                '}';
    }
}