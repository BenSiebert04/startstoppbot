package com.example.startstoppbot.repository;

import com.example.startstoppbot.model.Application;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {
    
    Optional<Application> findByName(String name);
    
    boolean existsByName(String name);
    
    @Query("SELECT a FROM Application a WHERE a.lastUpdate < :cutoffTime AND a.isOnline = true")
    List<Application> findOfflineApplications(@Param("cutoffTime") LocalDateTime cutoffTime);
    
    List<Application> findAllByOrderByNameAsc();
}