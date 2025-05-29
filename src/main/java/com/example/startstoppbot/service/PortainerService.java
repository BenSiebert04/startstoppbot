package com.example.startstoppbot.service;

import com.example.startstoppbot.model.ContainerInfo;
import com.example.startstoppbot.repository.ContainerInfoRepository;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
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
    private LocalDateTime tokenExpiry;
    private final ContainerInfoRepository containerInfoRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    public PortainerService(ContainerInfoRepository containerInfoRepository) {
        this.containerInfoRepository = containerInfoRepository;
    }

    private boolean authenticate() {
        try {
            System.out.println("Authentifiziere bei Portainer: " + portainerUrl);

            String authUrl = portainerUrl + "/api/auth";

            Map<String, String> authRequest = new HashMap<>();
            authRequest.put("Username", username);
            authRequest.put("Password", password);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(authRequest, headers);

            System.out.println("Auth Request URL: " + authUrl);
            System.out.println("Auth Request Body: " + authRequest);

            ResponseEntity<Map> response = restTemplate.postForEntity(authUrl, entity, Map.class);

            System.out.println("Auth Response Status: " + response.getStatusCode());
            System.out.println("Auth Response Body: " + response.getBody());

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                this.authToken = (String) response.getBody().get("jwt");
                // Token ist normalerweise 8 Stunden gültig
                this.tokenExpiry = LocalDateTime.now().plusHours(7); // Etwas früher erneuern
                System.out.println("Authentifizierung erfolgreich. Token erhalten.");
                return true;
            }
        } catch (Exception e) {
            System.err.println("Portainer Authentifizierung fehlgeschlagen: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    private boolean isTokenValid() {
        return authToken != null &&
                !authToken.isEmpty() &&
                tokenExpiry != null &&
                LocalDateTime.now().isBefore(tokenExpiry);
    }

    private HttpHeaders getAuthHeaders() {
        if (!isTokenValid()) {
            System.out.println("Token ungültig oder abgelaufen. Neue Authentifizierung erforderlich.");
            if (!authenticate()) {
                throw new RuntimeException("Portainer Authentifizierung fehlgeschlagen");
            }
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + authToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));

        System.out.println("Auth Headers gesetzt. Token: " + authToken.substring(0, Math.min(20, authToken.length())) + "...");

        return headers;
    }

    @Transactional
    public void aktualisiereDB() {
        System.out.println("=== Starte Datenbank-Aktualisierung ===");

        String endpointsUrl = portainerUrl + "/api/endpoints";
        HttpHeaders headers = getAuthHeaders();
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            System.out.println("Rufe Endpoints ab: " + endpointsUrl);
            System.out.println("Request Headers: " + headers);

            ResponseEntity<EndpointResponse[]> response = restTemplate.exchange(
                    endpointsUrl, HttpMethod.GET, entity, EndpointResponse[].class);

//            ResponseEntity<String> rawResponse = restTemplate.exchange(
//                    endpointsUrl, HttpMethod.GET, entity, String.class);
//            System.out.println("Raw Response: " + rawResponse.getBody());

            System.out.println("Response Status: " + response.getStatusCode());
            System.out.println("Response Headers: " + response.getHeaders());

            if (response.getBody() != null) {
                System.out.println("Anzahl Endpoints erhalten: " + response.getBody().length);

                for (int i = 0; i < response.getBody().length; i++) {
                    EndpointResponse endpoint = response.getBody()[i];
                    System.out.println("Endpoint " + i + ": " + endpoint);
                    System.out.println("  - ID: " + endpoint.getId());
                    System.out.println("  - Name: " + endpoint.getName());
                    System.out.println("  - Type: " + endpoint.getType());
                    System.out.println("  - URL: " + endpoint.getUrl());
                    System.out.println("  - Status: " + endpoint.getStatus());

                    if (endpoint.getSnapshots() != null) {
                        System.out.println("  - Snapshots: " + endpoint.getSnapshots().size());

                        for (int j = 0; j < endpoint.getSnapshots().size(); j++) {
                            EndpointSnapshot snapshot = endpoint.getSnapshots().get(j);
                            System.out.println("    Snapshot " + j + ":");

                            if (snapshot.getDockerSnapshotRaw() != null) {
                                List<ContainerResponse> containers = snapshot.getDockerSnapshotRaw().getContainers();
                                System.out.println("      - Container: " + (containers != null ? containers.size() : 0));

                                if (containers != null && !containers.isEmpty()) {
                                    processContainers(endpoint.getId(), containers);
                                } else {
                                    System.out.println("      - Keine Container in diesem Snapshot");
                                }
                            } else {
                                System.out.println("      - DockerSnapshotRaw ist null");
                            }
                        }
                    } else {
                        System.out.println("  - Keine Snapshots vorhanden");
                    }
                }
            } else {
                System.out.println("Response Body ist null!");
            }

            if (response.getStatusCode() != HttpStatus.OK) {
                System.err.println("Unerwarteter Status Code: " + response.getStatusCode());
            }

        } catch (HttpClientErrorException e) {
            System.err.println("HTTP Fehler beim Abrufen der Endpoints:");
            System.err.println("Status: " + e.getStatusCode());
            System.err.println("Response Body: " + e.getResponseBodyAsString());
            System.err.println("Headers: " + e.getResponseHeaders());

            // Bei 401/403 Token neu anfordern
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED || e.getStatusCode() == HttpStatus.FORBIDDEN) {
                System.out.println("Authentifizierung fehlgeschlagen. Versuche erneute Anmeldung...");
                this.authToken = null; // Token zurücksetzen
                this.tokenExpiry = null;
            }

        } catch (Exception e) {
            System.err.println("Allgemeiner Fehler beim Aktualisieren der DB: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("=== Datenbank-Aktualisierung beendet ===");
    }

    private void processContainers(Integer endpointId, List<ContainerResponse> containers) {
        System.out.println("Verarbeite " + containers.size() + " Container für Endpoint " + endpointId);

        for (ContainerResponse container : containers) {
            if (container.getNames() == null || container.getNames().isEmpty()) {
                System.err.println("Container ohne Namen übersprungen");
                continue;
            }
            try {
                // Container-Namen bereinigen (entfernt führenden '/')
                List<String> cleanedNames = container.getNames().stream()
                        .map(name -> name.startsWith("/") ? name.substring(1) : name)
                        .collect(Collectors.toList());

                String containerName = cleanedNames.get(0);
                System.out.println("  Verarbeite Container: " + containerName + " (Status: " + container.getState() + ")");

                // Prüfen ob Container bereits existiert (nach Name, da Name jetzt Primärschlüssel ist)
                Optional<ContainerInfo> existingContainer = containerInfoRepository.findById(containerName);

                ContainerInfo containerInfo;
                if (existingContainer.isPresent()) {
                    // Container existiert bereits - Container-ID und Status aktualisieren
                    containerInfo = existingContainer.get();
                    containerInfo.setContainerId(container.getId()); // Wichtig: Container-ID aktualisieren!
                    containerInfo.setStatus(container.getState());
                    containerInfo.setEndpointId(endpointId);
                    System.out.println("    -> Container aktualisiert (Discord: " + containerInfo.getDiscordEnabled() + ")");
                } else {
                    // Neuer Container - mit Standard-Discord-Berechtigung (false)
                    containerInfo = new ContainerInfo();
                    containerInfo.setName(containerName);
                    containerInfo.setContainerId(container.getId());
                    containerInfo.setStatus(container.getState());
                    containerInfo.setEndpointId(endpointId);
                    containerInfo.setDiscordEnabled(false); // Standard: Discord deaktiviert
                    System.out.println("    -> Neuer Container erstellt");
                }

                containerInfoRepository.save(containerInfo);

            } catch (Exception e) {
                System.err.println("Fehler beim Verarbeiten von Container: " + e.getMessage());
                e.printStackTrace();
            }
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

        System.out.println("Container-Aktion: " + action + " für " + containerName + " - URL: " + url);

        HttpHeaders headers = getAuthHeaders();
        headers.remove(HttpHeaders.CONTENT_TYPE);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, String.class);

            System.out.println("Container-Aktion Antwort: " + response.getStatusCode());

            if (response.getStatusCode() != HttpStatus.NO_CONTENT) {
                throw new RuntimeException("Aktion fehlgeschlagen: " + response.getStatusCode());
            }
        } catch (Exception e) {
            System.err.println("Fehler bei Container-Aktion: " + e.getMessage());
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

    // Debugging-Methode zum manuellen Testen
    public String testPortainerConnection() {
        try {
            if (!authenticate()) {
                return "Authentifizierung fehlgeschlagen";
            }

            String endpointsUrl = portainerUrl + "/api/endpoints";
            HttpHeaders headers = getAuthHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    endpointsUrl, HttpMethod.GET, entity, String.class);

            return "Verbindung erfolgreich. Status: " + response.getStatusCode() +
                    ", Body Länge: " + (response.getBody() != null ? response.getBody().length() : 0);

        } catch (Exception e) {
            return "Verbindungsfehler: " + e.getMessage();
        }
    }

    // DTOs für die Portainer-API-Antwort
    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class EndpointResponse {
        @JsonProperty("Id")
        private Integer id;

        @JsonProperty("Name")
        private String name;

        @JsonProperty("Type")
        private Integer type;

        @JsonProperty("URL")
        private String url;

        @JsonProperty("Status")
        private Integer status;

        @JsonProperty("Snapshots")
        private List<EndpointSnapshot> snapshots;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class EndpointSnapshot {
        @JsonProperty("DockerSnapshotRaw")
        private DockerSnapshotRaw dockerSnapshotRaw;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class DockerSnapshotRaw {
        @JsonProperty("Containers")
        private List<ContainerResponse> containers;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class ContainerResponse {
        @JsonProperty("Id")
        private String id;

        @JsonProperty("Names")
        private List<String> names;

        @JsonProperty("State")
        private String state;
    }
}
