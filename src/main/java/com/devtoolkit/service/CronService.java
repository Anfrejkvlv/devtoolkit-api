package com.devtoolkit.service;

import com.devtoolkit.config.GeminiFallbackExecutor;
import com.devtoolkit.model.ToolResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CronService {

    //private final ChatClient chatClient;
    private final GeminiFallbackExecutor executor;
    /**
     * FEATURE IA SIGNATURE : Description → Expression CRON
     * Ex: "tous les jours à 8h du matin sauf le weekend"
     *     → "0 0 8 * * MON-FRI"
     */
    @Cacheable(value = "cron-generate", key = "#description.hashCode() + #framework.hashCode()")
    public ToolResponse<Map<String, Object>> generateFromDescription(String description, String framework) {
        try {
            String prompt = """
                Génère une expression CRON pour : "%s"
                Framework cible : %s (spring|quartz|unix|kubernetes)
                
                Réponds UNIQUEMENT avec ce JSON (sans markdown) :
                {
                  "expression": "l'expression CRON complète",
                  "format": "spring-6|quartz|unix-5|unix-6",
                  "explanation": {
                    "seconds": "description du champ (si présent)",
                    "minutes": "description",
                    "hours": "description",
                    "dayOfMonth": "description",
                    "month": "description",
                    "dayOfWeek": "description",
                    "year": "description (si présent)"
                  },
                  "humanReadable": "phrase en français décrivant quand ça s'exécute",
                  "nextExecutions": ["5 prochaines dates d'exécution au format ISO"],
                  "springAnnotation": "@Scheduled(cron = \\"<expression>\\")",
                  "alternatives": ["autres expressions équivalentes si pertinent"]
                }
                """.formatted(description, framework);

//            String aiResponse = chatClient.prompt()
//                .user(prompt)
//                .call()
//                .content();
            String aiResponse=executor.executeForText(chatClient->
                    chatClient.prompt()
                            .user(prompt)
                            .call()
                            .chatResponse());

            Map<String, Object> result = parseJsonResponse(aiResponse);
            return ToolResponse.okWithAI(result);

        } catch (Exception e) {
            log.error("Erreur génération CRON IA: {}", e.getMessage());
            return ToolResponse.error("Génération IA échouée: " + e.getMessage());
        }
    }

    /**
     * Explique une expression CRON existante.
     */
    @Cacheable(value = "cron-explain", key = "#expression.hashCode()")
    public ToolResponse<Map<String, Object>> explain(String expression) {
        try {
            String prompt = """
                Explique cette expression CRON : "%s"
                
                Réponds UNIQUEMENT avec ce JSON (sans markdown) :
                {
                  "isValid": true/false,
                  "humanReadable": "quand ça s'exécute en langage naturel",
                  "frequency": "once|hourly|daily|weekly|monthly|custom",
                  "fields": { champ: "explication" },
                  "warnings": ["pièges ou cas limites"]
                }
                """.formatted(expression);

//            String aiResponse = chatClient.prompt().user(prompt).call().content();
            String aiResponse=executor.executeForText(chatClient->
                    chatClient.prompt()
                            .user(prompt)
                            .call()
                            .chatResponse());
            return ToolResponse.okWithAI(parseJsonResponse(aiResponse));

        } catch (Exception e) {
            return ToolResponse.error("Explication IA échouée: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJsonResponse(String json) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper =
                new com.fasterxml.jackson.databind.ObjectMapper();
            String cleaned = json.replaceAll("```json\\n?|```\\n?", "").trim();
            return mapper.readValue(cleaned, Map.class);
        } catch (Exception e) {
            return Map.of("raw", json, "parseError", true);
        }
    }
}
