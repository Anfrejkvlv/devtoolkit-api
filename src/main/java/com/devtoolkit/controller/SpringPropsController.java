package com.devtoolkit.controller;

import com.devtoolkit.model.ToolResponse;
import com.devtoolkit.service.SpringPropsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/spring-props")
@RequiredArgsConstructor
public class SpringPropsController {

    private final SpringPropsService springPropsService;

    /**
     * POST /api/v1/spring-props/generate
     * Body: {
     *   "description": "API REST avec PostgreSQL, Redis, JWT...",
     *   "profile": "dev|prod|test",
     *   "technologies": ["postgresql", "redis", "jwt", "actuator"]
     * }
     */
    @PostMapping("/generate")
    public ResponseEntity<ToolResponse<Map<String, Object>>> generate(
            @RequestBody Map<String, Object> body) {

        String description = (String) body.getOrDefault("description", "");
        String profile     = (String) body.getOrDefault("profile", "dev");

        @SuppressWarnings("unchecked")
        List<String> technologies = body.containsKey("technologies")
            ? (List<String>) body.get("technologies")
            : List.of();

        if (description.isBlank()) {
            return ResponseEntity.badRequest()
                .body(ToolResponse.error("Le champ 'description' est requis"));
        }
        ToolResponse<Map<String,Object>> response=springPropsService.generate(description, profile, technologies);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/v1/spring-props/review
     * Body: { "yaml": "server:\n  port: 8080\n..." }
     * → Spring AI audite le fichier et propose des améliorations
     */
    @PostMapping("/review")
    public ResponseEntity<ToolResponse<Map<String, Object>>> review(
            @RequestBody Map<String, String> body) {

        String yaml = body.getOrDefault("yaml", "").trim();

        if (yaml.isBlank()) {
            return ResponseEntity.badRequest()
                .body(ToolResponse.error("Le champ 'yaml' est requis"));
        }
        ToolResponse<Map<String,Object>> response=springPropsService.review(yaml);
        log.info("YAML RESPONSE",response);
        return ResponseEntity.ok(response);
    }
}
