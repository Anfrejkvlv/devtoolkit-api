package com.devtoolkit.controller;

import com.devtoolkit.model.ToolResponse;
import com.devtoolkit.service.CronService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/cron")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class CronController {

    private final CronService cronService;

    /**
     * POST /api/v1/cron/generate
     * Body: { "description": "tous les lundis à 9h", "framework": "spring" }
     */
    @PostMapping("/generate")
    public ResponseEntity<ToolResponse<Map<String, Object>>> generate(
            @RequestBody Map<String, String> body) {

        String description = body.getOrDefault("description", "");
        String framework   = body.getOrDefault("framework", "spring");

        if (description.isBlank()) {
            return ResponseEntity.badRequest()
                .body(ToolResponse.error("Le champ 'description' est requis"));
        }

        return ResponseEntity.ok(cronService.generateFromDescription(description, framework));
    }

    /**
     * POST /api/v1/cron/explain
     * Body: { "expression": "0 0 8 * * MON-FRI" }
     */
    @PostMapping("/explain")
    public ResponseEntity<ToolResponse<Map<String, Object>>> explain(
            @RequestBody Map<String, String> body) {

        String expression = body.getOrDefault("expression", "");

        if (expression.isBlank()) {
            return ResponseEntity.badRequest()
                .body(ToolResponse.error("Le champ 'expression' est requis"));
        }

        return ResponseEntity.ok(cronService.explain(expression));
    }
}
