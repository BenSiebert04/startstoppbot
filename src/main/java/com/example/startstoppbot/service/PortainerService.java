package com.example.startstoppbot.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class PortainerService {

    @Value("${portainer.url}")
    private String portainerUrl;

    @Value("${portainer.username}")
    private String username;

    @Value("${portainer.password}")
    private String password;

    private String authToken;
    private final RestTemplate restTemplate = new RestTemplate();

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

    public boolean startContainer(String containerId) {
        try {
            String startUrl = portainerUrl + "/api/endpoints/1/docker/containers/" + containerId + "/start";
            
            HttpEntity<String> entity = new HttpEntity<>("", getAuthHeaders());
            
            ResponseEntity<String> response = restTemplate.postForEntity(startUrl, entity, String.class);
            
            return response.getStatusCode() == HttpStatus.NO_CONTENT || 
                   response.getStatusCode() == HttpStatus.OK;
                   
        } catch (Exception e) {
            System.err.println("Fehler beim Starten des Containers " + containerId + ": " + e.getMessage());
            
            // Bei 401 Unauthorized erneut authentifizieren
            if (e.getMessage().contains("401")) {
                authToken = null;
                return startContainer(containerId); // Retry
            }
            return false;
        }
    }

    public boolean stopContainer(String containerId) {
        try {
            String stopUrl = portainerUrl + "/api/endpoints/1/docker/containers/" + containerId + "/stop";
            
            HttpEntity<String> entity = new HttpEntity<>("", getAuthHeaders());
            
            ResponseEntity<String> response = restTemplate.postForEntity(stopUrl, entity, String.class);
            
            return response.getStatusCode() == HttpStatus.NO_CONTENT || 
                   response.getStatusCode() == HttpStatus.OK;
                   
        } catch (Exception e) {
            System.err.println("Fehler beim Stoppen des Containers " + containerId + ": " + e.getMessage());
            
            // Bei 401 Unauthorized erneut authentifizieren
            if (e.getMessage().contains("401")) {
                authToken = null;
                return stopContainer(containerId); // Retry
            }
            return false;
        }
    }

    public boolean isContainerRunning(String containerId) {
        try {
            String inspectUrl = portainerUrl + "/api/endpoints/1/docker/containers/" + containerId + "/json";
            
            HttpEntity<String> entity = new HttpEntity<>("", getAuthHeaders());
            
            ResponseEntity<Map> response = restTemplate.exchange(inspectUrl, HttpMethod.GET, entity, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> state = (Map<String, Object>) response.getBody().get("State");
                return state != null && Boolean.TRUE.equals(state.get("Running"));
            }
            
        } catch (Exception e) {
            System.err.println("Fehler beim Prüfen des Container-Status " + containerId + ": " + e.getMessage());
            
            // Bei 401 Unauthorized erneut authentifizieren
            if (e.getMessage().contains("401")) {
                authToken = null;
                return isContainerRunning(containerId); // Retry
            }
        }
        return false;
    }
}