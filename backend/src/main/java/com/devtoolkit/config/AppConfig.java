package com.devtoolkit.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class AppConfig implements WebMvcConfigurer {

    /**
     * ChatClient Spring AI - point d'entrée pour tous les appels IA.
     * Le system prompt par défaut donne un contexte de dev tool assistant.
     */
    @Bean
    public ChatClient chatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel)
            .defaultSystem("""
                Tu es un assistant expert en développement logiciel intégré dans DevToolkit AI.
                Tu analyses du code, des tokens JWT, des expressions regex, des requêtes SQL, etc.
                Tes réponses sont TOUJOURS concises, précises, et orientées action.
                Réponds en JSON structuré sauf si explicitement demandé autrement.
                Langue de réponse : adapte-toi à la langue de l'utilisateur.
                """)
            .build();
    }

    /**
     * CORS - autorise Angular en dev (localhost:4200).
     * En production, le domaine du site : <a href="https://devtoolkit.ai">le backend</a>.
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOrigins("http://localhost:4200", "https://devtoolkit.ai")
            .allowedMethods("GET", "POST", "OPTIONS")
            .allowedHeaders("*")
            .maxAge(3600);
    }
}
