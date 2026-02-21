package com.devtoolkit.service;

import com.devtoolkit.config.GeminiFallbackExecutor;
import com.devtoolkit.model.ToolResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

@Service
@RequiredArgsConstructor
@Slf4j
public class RegexService {

    //private final ChatClient chatClient;
    private final GeminiFallbackExecutor executor;

    /**
     * Teste une regex contre une chaîne et retourne tous les matches.
     */
    public ToolResponse<Map<String, Object>> test(String pattern, String flags, String input) {
        try {
            int flagsMask = buildFlags(flags);
            Pattern regex = Pattern.compile(pattern, flagsMask);
            Matcher matcher = regex.matcher(input);

            List<Map<String, Object>> matches = new ArrayList<>();
            while (matcher.find()) {
                Map<String, Object> match = new LinkedHashMap<>();
                match.put("fullMatch", matcher.group(0));
                match.put("start", matcher.start());
                match.put("end", matcher.end());

                // Groupes de capture nommés et positionnels
                List<String> groups = new ArrayList<>();
                for (int i = 1; i <= matcher.groupCount(); i++) {
                    groups.add(matcher.group(i));
                }
                if (!groups.isEmpty()) match.put("groups", groups);

                matches.add(match);
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("pattern", pattern);
            result.put("flags", flags);
            result.put("matchCount", matches.size());
            result.put("matches", matches);
            result.put("isValid", true);

            return ToolResponse.ok(result);

        } catch (PatternSyntaxException e) {
            return ToolResponse.error("Regex invalide à l'index " + e.getIndex() + ": " + e.getDescription());
        }
    }

    /**
     * FEATURE IA UNIQUE : Génère une regex depuis une description en langage naturel.
     * Ex: "email valide" → /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/
     */
    public ToolResponse<Map<String, Object>> generateFromDescription(String description, String language) {
        try {
            String prompt = """
                Génère une expression régulière pour : "%s"
                Langage cible : %s
                
                Réponds UNIQUEMENT avec ce JSON (sans markdown) :
                {
                  "pattern": "la regex sans les délimiteurs /.../ ",
                  "flags": "flags recommandés (ex: gi)",
                  "explanation": "explication de chaque partie de la regex",
                  "examples": {
                    "matching": ["3 exemples qui correspondent"],
                    "notMatching": ["2 exemples qui ne correspondent pas"]
                  },
                  "alternatives": ["regex alternative plus simple si possible"],
                  "warnings": ["limitations ou cas limites à connaître"]
                }
                """.formatted(description, language);

//            String aiResponse = chatClient.prompt()
//                .user(prompt)
//                .call()
//                .content();
            String aiResponse=executor.execute(chatClient->
                    chatClient.prompt()
                            .user(prompt)
                            .call()
                            .content());

            Map<String, Object> result = parseJsonResponse(aiResponse);
            return ToolResponse.okWithAI(result);

        } catch (Exception e) {
            log.error("Erreur génération regex IA: {}", e.getMessage());
            return ToolResponse.error("Génération IA échouée: " + e.getMessage());
        }
    }

    private int buildFlags(String flags) {
        int mask = 0;
        if (flags == null) return mask;
        if (flags.contains("i")) mask |= Pattern.CASE_INSENSITIVE;
        if (flags.contains("m")) mask |= Pattern.MULTILINE;
        if (flags.contains("s")) mask |= Pattern.DOTALL;
        return mask;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJsonResponse(String json) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper =
                new com.fasterxml.jackson.databind.ObjectMapper();
            String cleaned = json.replaceAll("```json\\n?|```\\n?", "").trim();
            return mapper.readValue(cleaned, Map.class);
        } catch (Exception e) {
            return Map.of("raw", json);
        }
    }
}
