package com.devtoolkit.controller;

import com.devtoolkit.model.ToolResponse;
import com.devtoolkit.service.YamlJsonService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/yaml")
@RequiredArgsConstructor
public class YamlJsonController {

    private final YamlJsonService yamlJsonService;

    /**
     * POST /api/v1/yaml/to-yaml
     * Body: { "input": "{\"key\": \"value\"}" }
     */
    @PostMapping("/to-yaml")
    public ResponseEntity<ToolResponse<Map<String, Object>>> toYaml(
            @RequestBody Map<String, String> body) {

        String input = body.getOrDefault("input", "").trim();
        if (input.isBlank()) {
            return ResponseEntity.badRequest()
                .body(ToolResponse.error("Le champ 'input' est requis"));
        }

        return ResponseEntity.ok(yamlJsonService.jsonToYaml(input));
    }

    /**
     * POST /api/v1/yaml/to-json
     * Body: { "input": "key: value" }
     */
    @PostMapping("/to-json")
    public ResponseEntity<ToolResponse<Map<String, Object>>> toJson(
            @RequestBody Map<String, String> body) {

        String input = body.getOrDefault("input", "").trim();
        if (input.isBlank()) {
            return ResponseEntity.badRequest()
                .body(ToolResponse.error("Le champ 'input' est requis"));
        }

        return ResponseEntity.ok(yamlJsonService.yamlToJson(input));
    }

    /**
     * POST /api/v1/yaml/validate
     * Body: { "input": "...", "format": "yaml|json" }
     */
    @PostMapping("/validate")
    public ResponseEntity<ToolResponse<Map<String, Object>>> validate(
            @RequestBody Map<String, String> body) {

        String input  = body.getOrDefault("input", "").trim();
        String format = body.getOrDefault("format", "yaml");

        if (input.isBlank()) {
            return ResponseEntity.badRequest()
                .body(ToolResponse.error("Le champ 'input' est requis"));
        }

        return ResponseEntity.ok(yamlJsonService.validate(input, format));
    }
}
