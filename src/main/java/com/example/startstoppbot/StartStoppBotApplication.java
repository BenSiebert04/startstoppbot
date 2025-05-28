package com.example.startstoppbot;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Collections;

@SpringBootApplication
@EnableScheduling
@OpenAPIDefinition(
    info = @Info(
        title = "StartStopp Bot API",
        version = "1.0",
        description = "API für Discord Bot zur Container-Verwaltung"
    )
)
public class StartStoppBotApplication implements CommandLineRunner {

    @Value("${discord.bot.token}")
    private String botToken;

    @Autowired
    private DiscordSlashCommands slashCommands;

    public static void main(String[] args) {
        SpringApplication.run(StartStoppBotApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        JDA jda = JDABuilder.createDefault(botToken)
                .enableIntents(Collections.emptyList())
                .addEventListeners(slashCommands)
                .build();

        // Slash Commands registrieren
        jda.updateCommands().addCommands(
                Commands.slash("getserverstatuslist", "Zeigt alle Server mit Status und Spieleranzahl"),
                Commands.slash("getserverstatus", "Zeigt Status eines bestimmten Servers")
                        .addOption(OptionType.STRING, "name", "Name der Anwendung", true),
                Commands.slash("startserver", "Startet einen Server")
                        .addOption(OptionType.STRING, "name", "Name der Anwendung", true),
                Commands.slash("stopserver", "Stoppt einen Server (nur Admin)")
                        .addOption(OptionType.STRING, "name", "Name der Anwendung", true)
        ).queue();

        System.out.println("Discord Bot gestartet!");
    }

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins("*")
                        .allowedMethods("GET", "POST", "PUT", "DELETE");
            }
        };
    }
}