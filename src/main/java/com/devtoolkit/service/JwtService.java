package com.devtoolkit.service;

import com.devtoolkit.config.GeminiFallbackExecutor;
import com.devtoolkit.model.ToolResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class JwtService {

    //private final ChatClient chatClient;
    private final GeminiFallbackExecutor executor;

    /**
     * Décode un JWT sans validation de signature (mode inspection).
     * Résultat mis en cache 1h car le token ne change pas.
     */
    @Cacheable(value = "jwt-decode", key = "#token.hashCode()")
    public ToolResponse<Map<String, Object>> decode(String token) {
        try {
            String[] parts = token.trim().split("\\.");
            if (parts.length < 2 || parts.length > 3) {
                return ToolResponse.error("Format JWT invalide (attendu: header.payload.signature)");
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("header",  decodeBase64Json(parts[0]));
            result.put("payload", decodeBase64Json(parts[1]));
            result.put("hasSignature", parts.length == 3);
            result.put("rawToken", token);

            return ToolResponse.ok(result);

        } catch (Exception e) {
            log.warn("Erreur décodage JWT: {}", e.getMessage());
            return ToolResponse.error("Token JWT malformé: " + e.getMessage());
        }
    }

    /**
     * Analyse IA des claims JWT - explique chaque claim en langage naturel.
     * Coûteux → mise en cache agressive.
     */
    @Cacheable(value = "jwt-ai-explain", key = "#payload.hashCode()")
    public ToolResponse<Map<String, Object>> explainWithAI(String payload) {
        try {
            String prompt=buildPrompt(payload);

//            String aiResponse = chatClient.prompt()
//                .user(prompt)
//                .call()
//                .content();
            String aiResponse=executor.executeForText(chatClient->
                    chatClient.prompt(prompt)
                            .user("User")
                            .call()
                            .chatResponse());
            log.info(aiResponse);
            // Parse la réponse JSON de l'IA
            Map<String, Object> aiData = parseJsonResponse(aiResponse);

            var response=ToolResponse.okWithAI(aiData);
            log.info("RESPONSE: "+response);
            return response;

        } catch (Exception e) {
            log.error("Erreur analyse IA JWT: {}", e.getMessage());
            return ToolResponse.error("Analyse IA indisponible: " + e.getMessage());
        }
    }

    private String decodeBase64Json(String encoded) {
        // Padding Base64URL → Base64 standard
        String padded = encoded + "=".repeat((4 - encoded.length() % 4) % 4);
        byte[] decoded = Base64.getUrlDecoder().decode(padded);
        return new String(decoded, StandardCharsets.UTF_8);
    }

    public String buildPrompt(String prompt){
        return String.format("""
                Voici le payload d'un JWT :
                %s
                
                Analyse chaque claim et fournis un JSON avec ce format exact :
                {
                  "summary": "description en 1 phrase de ce que fait ce token",
                  "claims": {
                    "<nom_du_claim>": {
                      "description": "explication claire",
                      "value": "<valeur>",
                      "risk": "none|low|medium|high",
                      "hint": "conseil ou avertissement si pertinent"
                    }
                  },
                  "expiresIn": "durée lisible si exp présent, sinon null",
                  "securityFlags": ["liste des problèmes de sécurité détectés"]
                }
                """,prompt);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJsonResponse(String json) {
        // Jackson ObjectMapper injecté via Spring, ou parsing manuel
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper =
                new com.fasterxml.jackson.databind.ObjectMapper();
            // Nettoie les éventuels backticks markdown de l'IA
            String cleaned ;
            //cleaned= json.replaceAll("```json\\n?|```\\n?", "").trim();
            cleaned= json.replaceAll("```json|```","").trim();
            return mapper.readValue(cleaned, Map.class);
        } catch (Exception e) {
            return Map.of("raw", json, "parseError", "Impossible de parser la réponse IA");
        }
    }
}
