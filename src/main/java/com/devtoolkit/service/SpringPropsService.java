package com.devtoolkit.service;

import com.devtoolkit.config.GeminiFallbackExecutor;
import com.devtoolkit.model.ToolResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class SpringPropsService {

    //private final ChatClient chatClient;
    private final GeminiFallbackExecutor executor;

    /**
     * FEATURE IA SIGNATURE : génère un application.yml Spring Boot complet
     * depuis une description des besoins du projet.
     *
     * Ex: "API REST avec PostgreSQL, Redis pour le cache, JWT auth, emails SMTP"
     * → application.yml complet et commenté
     */
    @Cacheable(value = "spring-props-generate", key = "#description.hashCode() + #profile.hashCode()")
    public ToolResponse<Map<String, Object>> generate(
            String description,
            String profile,
            List<String> technologies) {

        try {
            String prompt = """
                Tu es un expert Spring Boot. Génère un fichier application.yml complet pour ce projet :
                
                Description : %s
                Profile cible : %s
                Technologies : %s
                
                Retourne UNIQUEMENT ce JSON (sans markdown) :
                {
                  "applicationYml": "le contenu YAML complet du fichier application.yml avec des commentaires explicatifs",
                  "propertiesCount": <nombre de propriétés configurées>,
                  "sections": ["liste des sections présentes (ex: datasource, redis, security...)"],
                  "placeholders": [
                    {
                      "property": "spring.datasource.url",
                      "envVar": "DATABASE_URL",
                      "description": "URL JDBC de la base de données",
                      "example": "jdbc:postgresql://localhost:5432/mydb"
                    }
                  ],
                  "dockerComposeHint": "snippet docker-compose.yml minimal pour démarrer les services requis",
                  "warnings": ["problèmes ou configurations à adapter"],
                  "requiredDependencies": [
                    { "groupId": "...", "artifactId": "...", "comment": "pourquoi" }
                  ]
                }
                
                Règles :
                - Utilise ${ENV_VAR:default_value} pour les secrets
                - Commente chaque section importante
                - Profil %s = valeurs appropriées (dev: localhost, prod: variables d'env)
                - Inclus toujours : server.port, spring.application.name, logging.level
                """.formatted(description, profile, String.join(", ", technologies), profile);

//            String aiResponse = chatClient.prompt()
//                .user(prompt)
//                .call()
//                .content();

            String aiResponse=executor.executeForText(chatClient->
                    chatClient.prompt()
                            .user(prompt)
                            .call()
                            .chatResponse());

            log.info("AI RESPONSE: {}", aiResponse);
            ToolResponse<Map<String,Object>> response=ToolResponse.okWithAI(parseJson(aiResponse));
            log.info("GENERATE RESPONSE",response);
            return response;

        } catch (Exception e) {
            log.error("Erreur génération Spring Props IA : {}", e.getMessage());
            return ToolResponse.error("Génération IA échouée : " + e.getMessage());
        }
    }

    /**
     * Analyse un application.yml existant et propose des améliorations.
     */
    @Cacheable(value = "spring-props-review", key = "#yaml.hashCode()")
    public ToolResponse<Map<String, Object>> review(String yaml) {
        try {
            if (yaml.length() > 8000) {
                return ToolResponse.error("Fichier trop long (max 8000 caractères)");
            }

            String prompt = """
                Analyse ce fichier application.yml Spring Boot et retourne UNIQUEMENT ce JSON (sans markdown) :
                {
                  "score": <note de qualité de 0 à 100>,
                  "issues": [
                    {
                      "property": "chemin.de.la.propriété",
                      "severity": "critical|warning|info",
                      "message": "description du problème",
                      "fix": "correction suggérée"
                    }
                  ],
                  "securityIssues": ["secrets en clair, configs non sécurisées..."],
                  "missingProperties": ["propriétés importantes non configurées"],
                  "detectedTechnologies": ["liste des techs détectées"],
                  "suggestions": ["autres améliorations"],
                  "optimizedYml": "version améliorée du YAML (ou null si OK)"
                }
                
                Fichier à analyser :
                %s
                """.formatted(yaml);

            //String aiResponse = chatClient.prompt().user(prompt).call().content();
            String aiResponse=executor.executeForText(chatClient->
                    chatClient.prompt()
                            .user(prompt)
                            .call()
                            .chatResponse());
            return ToolResponse.okWithAI(parseJson(aiResponse));

        } catch (Exception e) {
            log.error("Erreur review Spring Props IA : {}", e.getMessage());
            return ToolResponse.error("Analyse IA échouée : " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJson(String json) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper =
                new com.fasterxml.jackson.databind.ObjectMapper();
            //String cleaned = json.replaceAll("```json\\n?|```\\n?|```yaml\\n?", "").trim();
            String cleaned = json.replaceAll("```json|```|```yaml", "").trim();
            return mapper.readValue(cleaned, Map.class);
        } catch (Exception e) {
            return Map.of("raw", json, "parseError", true);
        }
    }
}
