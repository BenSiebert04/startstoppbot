package com.example.startstoppbot.service;

import com.example.startstoppbot.model.Application;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SchedulerService {

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private PortainerService portainerService;

    /**
     * Überprüft alle 5 Minuten, ob Container gestoppt werden müssen
     * Container werden gestoppt, wenn sie 30 Minuten nicht mehr online waren
     */
    @Scheduled(fixedRate = 300000) // 5 Minuten in Millisekunden
    public void checkAndStopInactiveContainers() {
        System.out.println("Überprüfe inaktive Container...");
        
        List<Application> offlineApps = applicationService.getOfflineApplications();
        
        for (Application app : offlineApps) {
            System.out.println("Container '" + app.getName() + "' ist seit 30+ Minuten inaktiv. Stoppe Container...");
            
            // Versuche den Container zu stoppen
            boolean stopped = portainerService.stopContainer(app.getContainerId());
            
            if (stopped) {
                // Markiere Anwendung als offline
                applicationService.markApplicationOffline(app);
                System.out.println("Container '" + app.getName() + "' erfolgreich gestoppt.");
            } else {
                System.err.println("Fehler beim Stoppen des Containers '" + app.getName() + "'");
            }
        }
        
        if (offlineApps.isEmpty()) {
            System.out.println("Keine inaktiven Container gefunden.");
        }
    }

    /**
     * Überprüft alle 2 Minuten den aktuellen Status der Container bei Portainer
     * und synchronisiert den lokalen Status
     */
    @Scheduled(fixedRate = 120000) // 2 Minuten in Millisekunden
    public void syncContainerStatus() {
        List<Application> allApps = applicationService.getAllApplications();
        
        for (Application app : allApps) {
            boolean isRunning = portainerService.isContainerRunning(app.getContainerId());
            
            // Wenn Container läuft, aber als offline markiert ist
            if (isRunning && !app.getIsOnline()) {
                app.setIsOnline(true);
                app.updateLastSeen();
                System.out.println("Container '" + app.getName() + "' ist wieder online - Status aktualisiert.");
            }
            // Wenn Container nicht läuft, aber als online markiert ist
            else if (!isRunning && app.getIsOnline()) {
                applicationService.markApplicationOffline(app);
                System.out.println("Container '" + app.getName() + "' ist offline - Status aktualisiert.");
            }
        }
    }
}