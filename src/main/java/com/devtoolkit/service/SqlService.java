package com.devtoolkit.service;

import com.devtoolkit.config.GeminiFallbackExecutor;
import com.devtoolkit.model.ToolResponse;
import com.github.vertical_blank.sqlformatter.SqlFormatter;
import com.github.vertical_blank.sqlformatter.languages.Dialect;
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
public class SqlService {

    //private final ChatClient chatClient;
    private final GeminiFallbackExecutor executor;

    /**
     * Formate une requête SQL selon le dialecte cible.
     * Traitement 100% local, pas d'appel IA.
     */
    public ToolResponse<Map<String, Object>> format(String sql, String dialect) {
        try {
            Dialect d = switch (dialect.toLowerCase()) {
                case "postgresql", "postgres" -> Dialect.PostgreSql;
                case "mysql"                  -> Dialect.MySql;
                case "tsql", "sqlserver"      -> Dialect.TSql;
                default                       -> Dialect.StandardSql;
            };

            String formatted = SqlFormatter.of(d).format(sql.trim());

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("formatted", formatted);
            result.put("dialect",   dialect);
            result.put("lineCount", formatted.lines().count());
            result.put("originalLength", sql.length());
            result.put("formattedLength", formatted.length());

            return ToolResponse.ok(result);

        } catch (Exception e) {
            log.warn("Erreur formatage SQL : {}", e.getMessage());
            return ToolResponse.error("Formatage échoué : " + e.getMessage());
        }
    }

    /**
     * FEATURE IA : analyse la requête SQL et propose des optimisations.
     * - Suggestions d'index
     * - Anti-patterns détectés (SELECT *, N+1, DISTINCT inutile…)
     * - Réécriture optimisée si possible
     */
    @Cacheable(value = "sql-optimize", key = "#sql.hashCode() + #dialect.hashCode()")
    public ToolResponse<Map<String, Object>> optimize(String sql, String dialect) {
        try {
            if (sql.length() > 5000) {
                return ToolResponse.error("Requête trop longue (max 5000 caractères)");
            }

            String prompt = """
                Tu es un expert DBA. Analyse cette requête SQL (%s) et retourne UNIQUEMENT ce JSON (sans markdown) :
                {
                  "score": <note de qualité de 0 à 100>,
                  "summary": "résumé en 1 phrase de ce que fait la requête",
                  "antiPatterns": [
                    { "pattern": "nom du problème", "description": "explication", "severity": "low|medium|high" }
                  ],
                  "indexSuggestions": [
                    {
                      "table": "nom_table",
                      "columns": ["col1", "col2"],
                      "reason": "pourquoi cet index aiderait",
                      "sql": "CREATE INDEX idx_name ON table(col1, col2);"
                    }
                  ],
                  "optimizedQuery": "version optimisée de la requête (ou null si déjà optimale)",
                  "optimizationNotes": "explication des changements effectués",
                  "estimatedImpact": "low|medium|high"
                }
                
                Requête à analyser :
                %s
                """.formatted(dialect, sql);

//            String aiResponse = chatClient.prompt()
//                .user(prompt)
//                .call()
//                .content();
            String aiResponse=executor.execute(chatClient->
                    chatClient.prompt()
                            .user(prompt)
                            .call()
                            .content());

            return ToolResponse.okWithAI(parseJson(aiResponse));

        } catch (Exception e) {
            log.error("Erreur optimisation SQL IA : {}", e.getMessage());
            return ToolResponse.error("Analyse IA échouée : " + e.getMessage());
        }
    }

    /**
     * Explique une requête SQL en langage naturel.
     */
    @Cacheable(value = "sql-explain", key = "#sql.hashCode()")
    public ToolResponse<Map<String, Object>> explain(String sql) {
        try {
            String prompt = """
                Explique cette requête SQL à un développeur junior. Retourne UNIQUEMENT ce JSON (sans markdown) :
                {
                  "plain": "explication en langage naturel (2-3 phrases)",
                  "steps": [
                    { "step": 1, "clause": "FROM/JOIN/WHERE/...", "description": "ce que fait cette partie" }
                  ],
                  "tablesInvolved": ["liste des tables"],
                  "operationType": "SELECT|INSERT|UPDATE|DELETE|DDL"
                }
                
                SQL : %s
                """.formatted(sql);

//            String aiResponse = chatClient.prompt().user(prompt).call().content();
            String aiResponse=executor.execute(chatClient->
                    chatClient.prompt()
                            .user(prompt)
                            .call()
                            .content());
            return ToolResponse.okWithAI(parseJson(aiResponse));

        } catch (Exception e) {
            return ToolResponse.error("Explication IA échouée : " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJson(String json) {
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
