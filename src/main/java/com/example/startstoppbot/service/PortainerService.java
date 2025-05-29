package com.example.startstoppbot.service;

import com.example.startstoppbot.model.ContainerInfo;
import com.example.startstoppbot.repository.ContainerInfoRepository;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class PortainerService {

    @Value("${portainer.url}")
    private String portainerUrl;

    @Value("${portainer.username}")
    private String username;

    @Value("${portainer.password}")
    private String password;

    private String authToken;
    private final ContainerInfoRepository containerInfoRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    public PortainerService(ContainerInfoRepository containerInfoRepository) {
        this.containerInfoRepository = containerInfoRepository;
    }

    private boolean authenticate() {
        try {
            String authUrl = portainerUrl + "/api/auth";

            Map<String, String> authRequest = new HashMap<>();
            authRequest.put("Username", username);
            authRequest.put("Password", password);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(authRequest, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(authUrl, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                this.authToken = (String) response.getBody().get("jwt");
                return true;
            }
        } catch (Exception e) {
            System.err.println("Portainer Authentifizierung fehlgeschlagen: " + e.getMessage());
        }
        return false;
    }

    private HttpHeaders getAuthHeaders() {
        if (authToken == null || authToken.isEmpty()) {
            if (!authenticate()) {
                throw new RuntimeException("Portainer Authentifizierung fehlgeschlagen");
            }
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + authToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    @Transactional
    public void aktualisiereDB() {
        String endpointsUrl = portainerUrl + "/api/endpoints";
        HttpHeaders headers = getAuthHeaders();
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<EndpointResponse[]> response = restTemplate.exchange(
                    endpointsUrl, HttpMethod.GET, entity, EndpointResponse[].class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                for (EndpointResponse endpoint : response.getBody()) {
                    if (endpoint.getSnapshots() != null && !endpoint.getSnapshots().isEmpty()) {
                        EndpointSnapshot snapshot = endpoint.getSnapshots().get(0);
                        if (snapshot.getDockerSnapshotRaw() != null) {
                            processContainers(endpoint.getId(), snapshot.getDockerSnapshotRaw().getContainers());
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Fehler beim Aktualisieren der DB: " + e.getMessage());
        }
    }

    private void processContainers(Integer endpointId, List<ContainerResponse> containers) {
        for (ContainerResponse container : containers) {
            // Container-Namen bereinigen (entfernt führenden '/')
            List<String> cleanedNames = container.getNames().stream()
                    .map(name -> name.startsWith("/") ? name.substring(1) : name)
                    .collect(Collectors.toList());

            String containerName = cleanedNames.get(0);

            // Prüfen ob Container bereits existiert (nach Name, da Name jetzt Primärschlüssel ist)
            Optional<ContainerInfo> existingContainer = containerInfoRepository.findById(containerName);

            ContainerInfo containerInfo;
            if (existingContainer.isPresent()) {
                // Container existiert bereits - Container-ID und Status aktualisieren
                containerInfo = existingContainer.get();
                containerInfo.setContainerId(container.getId()); // Wichtig: Container-ID aktualisieren!
                containerInfo.setStatus(container.getState());
                containerInfo.setEndpointId(endpointId);
                // discordEnabled bleibt unverändert - das ist der Grund für diese Änderung!
            } else {
                // Neuer Container - mit Standard-Discord-Berechtigung (false)
                containerInfo = new ContainerInfo();
                containerInfo.setName(containerName);
                containerInfo.setContainerId(container.getId());
                containerInfo.setStatus(container.getState());
                containerInfo.setEndpointId(endpointId);
                containerInfo.setDiscordEnabled(false); // Standard: Discord deaktiviert
            }

            containerInfoRepository.save(containerInfo);
        }
    }

    public void starteContainer(String containerName) {
        containerAktion(containerName, "start");
    }

    public void stoppeContainer(String containerName) {
        containerAktion(containerName, "stop");
    }

    private void containerAktion(String containerName, String action) {
        Optional<ContainerInfo> containerOpt = containerInfoRepository.findById(containerName);
        if (containerOpt.isEmpty()) {
            throw new RuntimeException("Container nicht gefunden: " + containerName);
        }

        ContainerInfo container = containerOpt.get();
        String url = String.format("%s/api/endpoints/%d/docker/containers/%s/%s",
                portainerUrl, container.getEndpointId(), container.getContainerId(), action);

        HttpHeaders headers = getAuthHeaders();
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, String.class);

            if (response.getStatusCode() != HttpStatus.NO_CONTENT) {
                throw new RuntimeException("Aktion fehlgeschlagen: " + response.getStatusCode());
            }
        } catch (Exception e) {
            throw new RuntimeException("Fehler bei Container-Aktion: " + e.getMessage());
        }
    }

    // Neue Methode: Nur Discord-fähige Container für Discord-Bot
    public List<ContainerInfo> getDiscordEnabledContainers() {
        return containerInfoRepository.findByDiscordEnabledTrue();
    }

    // Neue Methode: Container-Aktion nur wenn Discord-berechtigt
    public void starteContainerFuerDiscord(String containerName) {
        Optional<ContainerInfo> containerOpt = containerInfoRepository.findById(containerName);
        if (containerOpt.isEmpty() || !containerOpt.get().getDiscordEnabled()) {
            throw new RuntimeException("Container nicht gefunden oder nicht für Discord freigegeben: " + containerName);
        }
        starteContainer(containerName);
    }

    public void stoppeContainerFuerDiscord(String containerName) {
        Optional<ContainerInfo> containerOpt = containerInfoRepository.findById(containerName);
        if (containerOpt.isEmpty() || !containerOpt.get().getDiscordEnabled()) {
            throw new RuntimeException("Container nicht gefunden oder nicht für Discord freigegeben: " + containerName);
        }
        stoppeContainer(containerName);
    }

    // DTOs für die Portainer-API-Antwort
    @Getter
    @Setter
    private static class EndpointResponse {
        private Integer Id;
        private List<EndpointSnapshot> Snapshots;
    }

    @Getter
    @Setter
    private static class EndpointSnapshot {
        private DockerSnapshotRaw DockerSnapshotRaw;
    }

    @Getter
    @Setter
    private static class DockerSnapshotRaw {
        private List<ContainerResponse> Containers;
    }

    @Getter
    @Setter
    private static class ContainerResponse {
        private String Id;
        private List<String> Names;
        private String State;
    }
}