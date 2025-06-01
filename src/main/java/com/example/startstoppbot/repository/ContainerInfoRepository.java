package com.example.startstoppbot.repository;

import com.example.startstoppbot.model.ContainerInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ContainerInfoRepository extends JpaRepository<ContainerInfo, String> {
    // Name ist jetzt der Primärschlüssel, daher direkte findById Verwendung möglich
    // Optional<ContainerInfo> findById(String name) - bereits durch JpaRepository verfügbar

    // Alle Container die für Discord freigegeben sind
    List<ContainerInfo> findByDiscordEnabledTrue();

    // Container nach Discord-Status suchen
    List<ContainerInfo> findByDiscordEnabled(Boolean discordEnabled);

    // Löschen eines Containers nach Name (ID)
    @Modifying
    @Query("DELETE FROM ContainerInfo c WHERE c.name = :name")
    int deleteByName(@Param("name") String name);

    // Discord-Status für einen Container ändern (nach Name - ist jetzt die ID)
    @Modifying
    @Query("UPDATE ContainerInfo c SET c.discordEnabled = :enabled WHERE c.name = :name")
    int updateDiscordEnabledByName(@Param("name") String name, @Param("enabled") Boolean enabled);

    // Container nach Container-ID finden (für Fallback-Suche)
    Optional<ContainerInfo> findByContainerId(String containerId);

    // Spielerzahl-Update für einen Container
    @Modifying
    @Query("UPDATE ContainerInfo c SET c.currentPlayers = :currentPlayers, c.lastPlayerUpdate = :updateTime WHERE c.name = :name")
    int updatePlayerCountByName(@Param("name") String name, @Param("currentPlayers") Integer currentPlayers, @Param("updateTime") LocalDateTime updateTime);

    // Spielerzahl und maximale Spielerzahl für einen Container setzen
    @Modifying
    @Query("UPDATE ContainerInfo c SET c.currentPlayers = :currentPlayers, c.maxPlayers = :maxPlayers, c.lastPlayerUpdate = :updateTime WHERE c.name = :name")
    int updatePlayerCountWithMaxByName(@Param("name") String name, @Param("currentPlayers") Integer currentPlayers, @Param("maxPlayers") Integer maxPlayers, @Param("updateTime") LocalDateTime updateTime);

    // Container mit aktiven Spielern finden
    List<ContainerInfo> findByCurrentPlayersGreaterThan(Integer playerCount);

    // Container mit Spielerzahl zwischen Min und Max
    @Query("SELECT c FROM ContainerInfo c WHERE c.currentPlayers BETWEEN :minPlayers AND :maxPlayers")
    List<ContainerInfo> findByPlayerCountBetween(@Param("minPlayers") Integer minPlayers, @Param("maxPlayers") Integer maxPlayers);
}