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

import java.util.*;
import java.util.stream.Collectors;

@Service
public class DockerService {

    @Value("${docker.host:http://192.168.178.108:2375}")
    private String dockerHost;

    @Value("${docker.api.version:v1.49}")
    private String apiVersion;

    private final ContainerInfoRepository containerInfoRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    public DockerService(ContainerInfoRepository containerInfoRepository) {
        this.containerInfoRepository = containerInfoRepository;
    }

    private String getBaseUrl() {
        return dockerHost + "/" + apiVersion;
    }

    @Transactional
    public void aktualisiereDB() {
        System.out.println("=== Starte Datenbank-Aktualisierung mit Docker Engine API ===");

        String containersUrl = getBaseUrl() + "/containers/json?all=true";

        try {
            System.out.println("Rufe Container ab: " + containersUrl);

            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<DockerContainer[]> response = restTemplate.exchange(
                    containersUrl, HttpMethod.GET, entity, DockerContainer[].class);

            System.out.println("Response Status: " + response.getStatusCode());

            if (response.getBody() != null) {
                System.out.println("Anzahl Container erhalten: " + response.getBody().length);
                processContainers(Arrays.asList(response.getBody()));
            } else {
                System.out.println("Response Body ist null!");
            }

        } catch (HttpClientErrorException e) {
            System.err.println("HTTP Fehler beim Abrufen der Container:");
            System.err.println("Status: " + e.getStatusCode());
            System.err.println("Response Body: " + e.getResponseBodyAsString());

        } catch (Exception e) {
            System.err.println("Allgemeiner Fehler beim Aktualisieren der DB: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("=== Datenbank-Aktualisierung beendet ===");
    }

    private void processContainers(List<DockerContainer> containers) {
        System.out.println("Verarbeite " + containers.size() + " Container");

        for (DockerContainer container : containers) {
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

                // Prüfen ob Container bereits existiert
                Optional<ContainerInfo> existingContainer = containerInfoRepository.findById(containerName);

                ContainerInfo containerInfo;
                if (existingContainer.isPresent()) {
                    // Container existiert bereits - Container-ID und Status aktualisieren
                    containerInfo = existingContainer.get();
                    containerInfo.setContainerId(container.getId());
                    containerInfo.setStatus(container.getState());
                    containerInfo.setEndpointId(1); // Docker Engine API hat keine Endpoints, setze Standard
                    System.out.println("    -> Container aktualisiert (Discord: " + containerInfo.getDiscordEnabled() + ")");
                } else {
                    // Neuer Container - mit Standard-Discord-Berechtigung (false)
                    containerInfo = new ContainerInfo();
                    containerInfo.setName(containerName);
                    containerInfo.setContainerId(container.getId());
                    containerInfo.setStatus(container.getState());
                    containerInfo.setEndpointId(1); // Standard Endpoint für Docker Engine API
                    containerInfo.setDiscordEnabled(false); // Standard: Discord deaktiviert
                    System.out.println("    -> Neuer Container erstellt");
                }

                containerInfoRepository.save(containerInfo);

            } catch (Exception e) {
                System.err.println("Fehler beim Verarbeiten von Container: " + e.getMessage());
                e.printStackTrace();
            }
        }

        deleteOldContainers(containers);
    }

    private void deleteOldContainers(List<DockerContainer> containers) {
        Set<String> currentContainerNames = containers.stream()
                .flatMap(c -> c.getNames().stream())
                .map(name -> name.startsWith("/") ? name.substring(1) : name)
                .collect(Collectors.toSet());

        List<ContainerInfo> allContainers = containerInfoRepository.findAll();
        for (ContainerInfo container : allContainers) {
            if (!currentContainerNames.contains(container.getName())) {
                System.out.println("Lösche alten Container: " + container.getName());
                containerInfoRepository.delete(container);
            }
        }
    }

    public void starteContainer(String containerName) {
        containerAktion(containerName, "start");
    }

    public void stoppeContainer(String containerName) {
        containerAktion(containerName, "stop");
    }

    public void restartContainer(String containerName) {
        containerAktion(containerName, "restart");
    }

    private void containerAktion(String containerName, String action) {
        Optional<ContainerInfo> containerOpt = containerInfoRepository.findById(containerName);
        if (containerOpt.isEmpty()) {
            throw new RuntimeException("Container nicht gefunden: " + containerName);
        }

        ContainerInfo container = containerOpt.get();
        String url = String.format("%s/containers/%s/%s",
                getBaseUrl(), container.getContainerId(), action);

        System.out.println("Container-Aktion: " + action + " für " + containerName + " - URL: " + url);

        HttpHeaders headers = new HttpHeaders();
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, String.class);

            System.out.println("Container-Aktion Antwort: " + response.getStatusCode());

            // Docker API gibt 204 No Content bei erfolgreichem Start/Stop zurück
            if (response.getStatusCode() != HttpStatus.NO_CONTENT) {
                throw new RuntimeException("Aktion fehlgeschlagen: " + response.getStatusCode());
            }

            System.out.println("Container " + containerName + " erfolgreich " + action + " ausgeführt");

        } catch (HttpClientErrorException e) {
            System.err.println("HTTP Fehler bei Container-Aktion:");
            System.err.println("Status: " + e.getStatusCode());
            System.err.println("Response: " + e.getResponseBodyAsString());

            // Spezifische Fehlermeldungen
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new RuntimeException("Container nicht gefunden oder bereits gelöscht");
            } else if (e.getStatusCode() == HttpStatus.NOT_MODIFIED) {
                throw new RuntimeException("Container ist bereits in dem gewünschten Zustand");
            } else {
                throw new RuntimeException("Container-Aktion fehlgeschlagen: " + e.getMessage());
            }
        } catch (Exception e) {
            System.err.println("Fehler bei Container-Aktion: " + e.getMessage());
            throw new RuntimeException("Fehler bei Container-Aktion: " + e.getMessage());
        }
    }

    // Container-Status direkt abfragen
    public String getContainerStatus(String containerName) {
        Optional<ContainerInfo> containerOpt = containerInfoRepository.findById(containerName);
        if (containerOpt.isEmpty()) {
            throw new RuntimeException("Container nicht gefunden: " + containerName);
        }

        ContainerInfo container = containerOpt.get();
        String url = String.format("%s/containers/%s/json",
                getBaseUrl(), container.getContainerId());

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<DockerContainerInspect> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, DockerContainerInspect.class);

            if (response.getBody() != null && response.getBody().getState() != null) {
                return response.getBody().getState().getStatus();
            }

            return "unknown";

        } catch (Exception e) {
            System.err.println("Fehler beim Abrufen des Container-Status: " + e.getMessage());
            return "error";
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
    public String testDockerConnection() {
        try {
            String versionUrl = getBaseUrl() + "/version";

            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    versionUrl, HttpMethod.GET, entity, String.class);

            return "Verbindung erfolgreich. Status: " + response.getStatusCode() +
                    ", Docker Version verfügbar";

        } catch (Exception e) {
            return "Verbindungsfehler: " + e.getMessage();
        }
    }

    public Set<String> getAllContainerNames() {
        System.out.println("Hole alle Container-Namen von Docker...");

        String containersUrl = getBaseUrl() + "/containers/json?all=true";

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<DockerContainer[]> response = restTemplate.exchange(
                    containersUrl, HttpMethod.GET, entity, DockerContainer[].class);

            if (response.getBody() != null) {
                Set<String> containerNames = new HashSet<>();

                for (DockerContainer container : response.getBody()) {
                    if (container.getNames() != null && !container.getNames().isEmpty()) {
                        // Container-Namen bereinigen (entfernt führenden '/')
                        List<String> cleanedNames = container.getNames().stream()
                                .map(name -> name.startsWith("/") ? name.substring(1) : name)
                                .collect(Collectors.toList());

                        containerNames.add(cleanedNames.get(0));
                    }
                }

                System.out.println("Gefundene Container-Namen: " + containerNames.size());
                return containerNames;
            }

            return new HashSet<>();

        } catch (HttpClientErrorException e) {
            System.err.println("HTTP Fehler beim Abrufen aller Container-Namen:");
            System.err.println("Status: " + e.getStatusCode());
            System.err.println("Response Body: " + e.getResponseBodyAsString());
            return new HashSet<>();

        } catch (Exception e) {
            System.err.println("Fehler beim Abrufen aller Container-Namen: " + e.getMessage());
            return new HashSet<>();
        }
    }

    // DTOs für die Docker Engine API-Antwort
    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class DockerContainer {
        @JsonProperty("Id")
        private String id;

        @JsonProperty("Names")
        private List<String> names;

        @JsonProperty("State")
        private String state;

        @JsonProperty("Status")
        private String status;

        @JsonProperty("Image")
        private String image;

        @JsonProperty("Command")
        private String command;

        @JsonProperty("Created")
        private Long created;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class DockerContainerInspect {
        @JsonProperty("Id")
        private String id;

        @JsonProperty("Name")
        private String name;

        @JsonProperty("State")
        private ContainerState state;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class ContainerState {
        @JsonProperty("Status")
        private String status;

        @JsonProperty("Running")
        private Boolean running;

        @JsonProperty("StartedAt")
        private String startedAt;

        @JsonProperty("FinishedAt")
        private String finishedAt;
    }
}