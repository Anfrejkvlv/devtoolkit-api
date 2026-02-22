package com.devtoolkit.controller;

import com.devtoolkit.config.ModelFallbackManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/models")
public class ModelStatusController {

    private final ModelFallbackManager fallbackManager;

    public ModelStatusController(ModelFallbackManager fallbackManager) {
        this.fallbackManager = fallbackManager;
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        return ResponseEntity.ok(Map.of(
                "availableModel", fallbackManager.getAvailableModel(),
                "exhaustedModels", fallbackManager.getExhaustedModels()
        ));
    }
}
