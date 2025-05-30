package com.example.startstoppbot;

import com.example.startstoppbot.model.ContainerInfo;
import com.example.startstoppbot.service.DockerService;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class DiscordSlashCommands extends ListenerAdapter {

    @Autowired
    private DockerService dockerService;

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

        try {
            List<ContainerInfo> containers = dockerService.getDiscordEnabledContainers();

            if (containers.isEmpty()) {
                event.getHook().editOriginal("Keine Container für Discord freigegeben.").queue();
                return;
            }

            EmbedBuilder embedBuilder = new EmbedBuilder()
                    .setTitle("🖥️ Server Status Liste")
                    .setColor(Color.BLUE)
                    .setDescription("Alle für Discord freigegebene Container:");

            StringBuilder statusList = new StringBuilder();
            for (ContainerInfo container : containers) {
                String statusEmoji = getStatusEmoji(container.getStatus());
                String playerInfo = "";

                if (container.getCurrentPlayers() != null) {
                    playerInfo = String.format(" | 👥 %d", container.getCurrentPlayers());
                    if (container.getMaxPlayers() != null) {
                        playerInfo += "/" + container.getMaxPlayers();
                    }
                }

                statusList.append(String.format("%s **%s** - %s%s\n",
                        statusEmoji, container.getName(), container.getStatus(), playerInfo));
            }

            embedBuilder.addField("Container", statusList.toString(), false);
            event.getHook().editOriginalEmbeds(embedBuilder.build()).queue();

        } catch (Exception e) {
            event.getHook().editOriginal("❌ Fehler beim Abrufen der Container-Liste: " + e.getMessage()).queue();
        }
    }

    private void handleGetServerStatus(SlashCommandInteractionEvent event) {
        event.deferReply().queue();

        OptionMapping containerOption = event.getOption("name");
        if (containerOption == null) {
            event.getHook().editOriginal("❌ Container-Name ist erforderlich.").queue();
            return;
        }

        String containerName = containerOption.getAsString();

        try {
            List<ContainerInfo> containers = dockerService.getDiscordEnabledContainers();
            ContainerInfo container = containers.stream()
                    .filter(c -> c.getName().equalsIgnoreCase(containerName))
                    .findFirst()
                    .orElse(null);

            if (container == null) {
                event.getHook().editOriginal("❌ Container '" + containerName + "' nicht gefunden oder nicht für Discord freigegeben.").queue();
                return;
            }

            String statusEmoji = getStatusEmoji(container.getStatus());
            Color embedColor = getStatusColor(container.getStatus());

            EmbedBuilder embedBuilder = new EmbedBuilder()
                    .setTitle("🖥️ Server Status: " + container.getName())
                    .setColor(embedColor)
                    .addField("Status", statusEmoji + " " + container.getStatus(), true)
                    .addField("Container ID", container.getContainerId(), true)
                    .addField("Endpoint ID", container.getEndpointId().toString(), true);

            if (container.getCurrentPlayers() != null) {
                String playerInfo = container.getCurrentPlayers().toString();
                if (container.getMaxPlayers() != null) {
                    playerInfo += " / " + container.getMaxPlayers();
                }
                embedBuilder.addField("👥 Spieler", playerInfo, true);
            }

            if (container.getLastPlayerUpdate() != null) {
                String lastUpdate = container.getLastPlayerUpdate()
                        .format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"));
                embedBuilder.addField("🕐 Letzte Aktualisierung", lastUpdate, false);
            }

            event.getHook().editOriginalEmbeds(embedBuilder.build()).queue();

        } catch (Exception e) {
            event.getHook().editOriginal("❌ Fehler beim Abrufen des Container-Status: " + e.getMessage()).queue();
        }
    }

    private void handleStartServer(SlashCommandInteractionEvent event) {
        event.deferReply().queue();

        OptionMapping containerOption = event.getOption("name");
        if (containerOption == null) {
            event.getHook().editOriginal("❌ Container-Name ist erforderlich.").queue();
            return;
        }

        String containerName = containerOption.getAsString();

        try {
            dockerService.starteContainerFuerDiscord(containerName);

            EmbedBuilder embedBuilder = new EmbedBuilder()
                    .setTitle("✅ Server gestartet")
                    .setColor(Color.GREEN)
                    .setDescription("Container **" + containerName + "** wird gestartet...")
                    .addField("ℹ️ Hinweis", "Es kann einige Sekunden dauern, bis der Server vollständig hochgefahren ist.", false);

            event.getHook().editOriginalEmbeds(embedBuilder.build()).queue();

        } catch (Exception e) {
            EmbedBuilder embedBuilder = new EmbedBuilder()
                    .setTitle("❌ Fehler beim Starten")
                    .setColor(Color.RED)
                    .setDescription("Container **" + containerName + "** konnte nicht gestartet werden.")
                    .addField("Fehlergrund", e.getMessage(), false);

            event.getHook().editOriginalEmbeds(embedBuilder.build()).queue();
        }
    }

    private void handleStopServer(SlashCommandInteractionEvent event) {
        event.deferReply().queue();

        OptionMapping containerOption = event.getOption("name");
        if (containerOption == null) {
            event.getHook().editOriginal("❌ Container-Name ist erforderlich.").queue();
            return;
        }

        String containerName = containerOption.getAsString();

        try {
            dockerService.stoppeContainerFuerDiscord(containerName);

            EmbedBuilder embedBuilder = new EmbedBuilder()
                    .setTitle("🛑 Server gestoppt")
                    .setColor(Color.ORANGE)
                    .setDescription("Container **" + containerName + "** wird gestoppt...")
                    .addField("ℹ️ Hinweis", "Es kann einige Sekunden dauern, bis der Server vollständig heruntergefahren ist.", false);

            event.getHook().editOriginalEmbeds(embedBuilder.build()).queue();

        } catch (Exception e) {
            EmbedBuilder embedBuilder = new EmbedBuilder()
                    .setTitle("❌ Fehler beim Stoppen")
                    .setColor(Color.RED)
                    .setDescription("Container **" + containerName + "** konnte nicht gestoppt werden.")
                    .addField("Fehlergrund", e.getMessage(), false);

            event.getHook().editOriginalEmbeds(embedBuilder.build()).queue();
        }
    }

    private String getStatusEmoji(String status) {
        switch (status.toLowerCase()) {
            case "running":
                return "🟢";
            case "exited":
            case "stopped":
                return "🔴";
            case "paused":
                return "🟡";
            case "restarting":
                return "🔄";
            default:
                return "⚪";
        }
    }

    private Color getStatusColor(String status) {
        switch (status.toLowerCase()) {
            case "running":
                return Color.GREEN;
            case "exited":
            case "stopped":
                return Color.RED;
            case "paused":
                return Color.ORANGE;
            case "restarting":
                return Color.YELLOW;
            default:
                return Color.GRAY;
        }
    }
}