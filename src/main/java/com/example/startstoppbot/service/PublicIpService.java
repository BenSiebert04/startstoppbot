package com.example.startstoppbot.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

@Service
public class PublicIpService {

    private final RestTemplate restTemplate;

    public PublicIpService() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Ruft die öffentliche IP-Adresse über einen externen Service ab
     * @return Die öffentliche IP-Adresse als String
     * @throws RuntimeException wenn die IP nicht abgerufen werden kann
     */
    public String getPublicIp() {
        try {
            // Primärer Service: ipify.org
            String ip = restTemplate.getForObject("https://api.ipify.org", String.class);
            if (ip != null && !ip.trim().isEmpty()) {
                return ip.trim();
            }
        } catch (RestClientException e) {
            System.err.println("Fehler beim Abrufen der IP von ipify.org: " + e.getMessage());
        }

        try {
            // Fallback Service: httpbin.org
            String response = restTemplate.getForObject("https://httpbin.org/ip", String.class);
            if (response != null) {
                // Response Format: {"origin":"123.456.789.123"}
                String ip = response.replaceAll(".*\"origin\":\"([^\"]+)\".*", "$1");
                if (!ip.equals(response) && !ip.trim().isEmpty()) {
                    return ip.trim();
                }
            }
        } catch (RestClientException e) {
            System.err.println("Fehler beim Abrufen der IP von httpbin.org: " + e.getMessage());
        }

        try {
            // Zweiter Fallback: icanhazip.com
            String ip = restTemplate.getForObject("https://icanhazip.com", String.class);
            if (ip != null && !ip.trim().isEmpty()) {
                return ip.trim();
            }
        } catch (RestClientException e) {
            System.err.println("Fehler beim Abrufen der IP von icanhazip.com: " + e.getMessage());
        }

        throw new RuntimeException("Konnte öffentliche IP-Adresse nicht abrufen. Alle Services sind nicht erreichbar.");
    }


}