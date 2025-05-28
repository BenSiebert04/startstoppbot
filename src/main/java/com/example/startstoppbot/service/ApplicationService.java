package com.example.startstoppbot.service;

import com.example.startstoppbot.model.Application;
import com.example.startstoppbot.repository.ApplicationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ApplicationService {

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private PortainerService portainerService;

    public List<Application> getAllApplications() {
        return applicationRepository.findAllByOrderByNameAsc();
    }

    public Optional<Application> getApplicationByName(String name) {
        return applicationRepository.findByName(name);
    }

    public Application createOrUpdateApplication(String name, String containerId, Integer currentPlayers) {
        Optional<Application> existingApp = applicationRepository.findByName(name);
        
        if (existingApp.isPresent()) {
            Application app = existingApp.get();
            app.setCurrentPlayers(currentPlayers);
            app.updateLastSeen();
            if (containerId != null && !containerId.isEmpty()) {
                app.setContainerId(containerId);
            }
            return applicationRepository.save(app);
        } else {
            Application newApp = new Application(name, containerId, currentPlayers);
            return applicationRepository.save(newApp);
        }
    }

    public Application updatePlayerCount(String name, Integer currentPlayers) {
        Optional<Application> existingApp = applicationRepository.findByName(name);
        
        if (existingApp.isPresent()) {
            Application app = existingApp.get();
            app.setCurrentPlayers(currentPlayers);
            app.updateLastSeen();
            return applicationRepository.save(app);
        } else {
            throw new RuntimeException("Anwendung '" + name + "' nicht gefunden");
        }
    }

    public List<Application> getOfflineApplications() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(30);
        return applicationRepository.findOfflineApplications(cutoffTime);
    }

    public void markApplicationOffline(Application application) {
        application.setIsOnline(false);
        application.setCurrentPlayers(0);
        applicationRepository.save(application);
    }

    public boolean startServer(String name) {
        Optional<Application> appOpt = applicationRepository.findByName(name);
        if (appOpt.isPresent()) {
            Application app = appOpt.get();
            boolean success = portainerService.startContainer(app.getContainerId());
            if (success) {
                app.setIsOnline(true);
                app.updateLastSeen();
                applicationRepository.save(app);
            }
            return success;
        }
        return false;
    }

    public boolean stopServer(String name) {
        Optional<Application> appOpt = applicationRepository.findByName(name);
        if (appOpt.isPresent()) {
            Application app = appOpt.get();
            boolean success = portainerService.stopContainer(app.getContainerId());
            if (success) {
                markApplicationOffline(app);
            }
            return success;
        }
        return false;
    }

    public boolean applicationExists(String name) {
        return applicationRepository.existsByName(name);
    }
}