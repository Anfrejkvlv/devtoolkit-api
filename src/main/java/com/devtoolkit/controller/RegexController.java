package com.devtoolkit.controller;

import com.devtoolkit.model.ToolResponse;
import com.devtoolkit.service.RegexService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/regex")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class RegexController {

    private final RegexService regexService;

    /**
     * POST /api/v1/regex/test
     * Body: { "pattern": "\\d+", "flags": "gm", "input": "test 123 abc 456" }
     */
    @PostMapping("/test")
    public ResponseEntity<ToolResponse<Map<String, Object>>> test(
            @RequestBody Map<String, String> body) {

        String pattern = body.getOrDefault("pattern", "");
        String flags   = body.getOrDefault("flags", "");
        String input   = body.getOrDefault("input", "");

        if (pattern.isBlank()) {
            return ResponseEntity.badRequest()
                .body(ToolResponse.error("Le champ 'pattern' est requis"));
        }

        return ResponseEntity.ok(regexService.test(pattern, flags, input));
    }

    /**
     * POST /api/v1/regex/generate
     * Body: { "description": "valider un email", "language": "java" }
     * → Spring AI génère la regex optimale
     */
    @PostMapping("/generate")
    public ResponseEntity<ToolResponse<Map<String, Object>>> generate(
            @RequestBody Map<String, String> body) {

        String description = body.getOrDefault("description", "");
        String language    = body.getOrDefault("language", "javascript");

        if (description.isBlank()) {
            return ResponseEntity.badRequest()
                .body(ToolResponse.error("Le champ 'description' est requis"));
        }

        return ResponseEntity.ok(regexService.generateFromDescription(description, language));
    }
}
