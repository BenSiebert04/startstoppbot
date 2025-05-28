package com.example.startstoppbot;

import com.example.startstoppbot.model.Application;
import com.example.startstoppbot.service.ApplicationService;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Component
public class DiscordSlashCommands extends ListenerAdapter {

    @Autowired
    private ApplicationService applicationService;

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        switch (event.getName()) {
            case "getserverstatuslist":
                handleGetServerStatusList(event);
                break;
            case "getserverstatus":
                handleGetServerStatus(event);
                break;
            case "startserver":
                handleStartServer(event);
                break;
            case "stopserver":
                handleStopServer(event);
                break;
        }
    }

    private void handleGetServerStatusList(SlashCommandInteractionEvent event) {
        event.deferReply().queue();

        List<Application> applications = applicationService.getAllApplications();

        if (applications.isEmpty()) {
            event.getHook().editOriginal("❌ Keine Server registriert.").queue();
            return;
        }

        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("🖥️ Server Status Liste");
        embed.setColor(Color.BLUE);

        StringBuilder statusList = new StringBuilder();
        for (Application app : applications) {
            String status = app.getIsOnline() ? "🟢 Online" : "🔴 Offline";
            String players = app.getIsOnline() ? String.valueOf(app.getCurrentPlayers()) : "0";
            String lastUpdate = app.getLastUpdate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
            
            statusList.append("**").append(app.getName()).append("**\n");
            statusList.append("Status: ").append(status).append("\n");
            statusList.append("Spieler: ").append(players).append("\n");
            statusList.append("Letzte Aktualisierung: ").append(lastUpdate).append("\n\n");
        }

        embed.setDescription(statusList.toString());
        embed.setFooter("Aktualisiert: " + java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")));

        event.getHook().editOriginalEmbeds(embed.build()).queue();
    }

    private void handleGetServerStatus(SlashCommandInteractionEvent event) {
        event.deferReply().queue();

        String serverName = event.getOption("name").getAsString();
        Optional<Application> appOpt = applicationService.getApplicationByName(serverName);

        if (appOpt.isEmpty()) {
            event.getHook().editOriginal("❌ Server '" + serverName + "' nicht gefunden.").queue();
            return;
        }

        Application app = appOpt.get();
        
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("🖥️ Server Status: " + app.getName());
        
        if (app.getIsOnline()) {
            embed.setColor(Color.GREEN);
            embed.addField("Status", "🟢 Online", true);
            embed.addField("Spieler", String.valueOf(app.getCurrentPlayers()), true);
        } else {
            embed.setColor(Color.RED);
            embed.addField("Status", "🔴 Offline", true);
            embed.addField("Spieler", "0", true);
        }
        
        embed.addField("Container ID", app.getContainerId(), false);
        embed.addField("Letzte Aktualisierung", 
                app.getLastUpdate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")), false);
        
        event.getHook().editOriginalEmbeds(embed.build()).queue();
    }

    private void handleStartServer(SlashCommandInteractionEvent event) {
        event.deferReply().queue();

        String serverName = event.getOption("name").getAsString();
        
        if (!applicationService.applicationExists(serverName)) {
            event.getHook().editOriginal("❌ Server '" + serverName + "' nicht gefunden.").queue();
            return;
        }

        Optional<Application> appOpt = applicationService.getApplicationByName(serverName);
        if (appOpt.isPresent() && appOpt.get().getIsOnline()) {
            event.getHook().editOriginal("⚠️ Server '" + serverName + "' läuft bereits.").queue();
            return;
        }

        event.getHook().editOriginal("🔄 Starte Server '" + serverName + "'...").queue();

        if (applicationService.startServer(serverName)) {
            EmbedBuilder embed = new EmbedBuilder();
            embed.setTitle("✅ Server gestartet");
            embed.setDescription("Server '" + serverName + "' wurde erfolgreich gestartet!");
            embed.setColor(Color.GREEN);
            embed.setTimestamp(java.time.Instant.now());
            
            event.getHook().editOriginalEmbeds(embed.build()).queue();
        } else {
            EmbedBuilder embed = new EmbedBuilder();
            embed.setTitle("❌ Fehler beim Starten");
            embed.setDescription("Server '" + serverName + "' konnte nicht gestartet werden.");
            embed.setColor(Color.RED);
            embed.setTimestamp(java.time.Instant.now());
            
            event.getHook().editOriginalEmbeds(embed.build()).queue();
        }
    }

    private void handleStopServer(SlashCommandInteractionEvent event) {
        event.deferReply().queue();

        // Admin-Berechtigung prüfen
        if (!event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
            event.getHook().editOriginal("❌ Du benötigst Administrator-Berechtigung für diesen Befehl.").queue();
            return;
        }

        String serverName = event.getOption("name").getAsString();
        
        if (!applicationService.applicationExists(serverName)) {
            event.getHook().editOriginal("❌ Server '" + serverName + "' nicht gefunden.").queue();
            return;
        }

        Optional<Application> appOpt = applicationService.getApplicationByName(serverName);
        if (appOpt.isPresent() && !appOpt.get().getIsOnline()) {
            event.getHook().editOriginal("⚠️ Server '" + serverName + "' ist bereits offline.").queue();
            return;
        }

        event.getHook().editOriginal("🔄 Stoppe Server '" + serverName + "'...").queue();

        if (applicationService.stopServer(serverName)) {
            EmbedBuilder embed = new EmbedBuilder();
            embed.setTitle("✅ Server gestoppt");
            embed.setDescription("Server '" + serverName + "' wurde erfolgreich gestoppt!");
            embed.setColor(Color.ORANGE);
            embed.setTimestamp(java.time.Instant.now());
            
            event.getHook().editOriginalEmbeds(embed.build()).queue();
        } else {
            EmbedBuilder embed = new EmbedBuilder();
            embed.setTitle("❌ Fehler beim Stoppen");
            embed.setDescription("Server '" + serverName + "' konnte nicht gestoppt werden.");
            embed.setColor(Color.RED);
            embed.setTimestamp(java.time.Instant.now());
            
            event.getHook().editOriginalEmbeds(embed.build()).queue();
        }
    }
}