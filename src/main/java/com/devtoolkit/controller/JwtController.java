package com.devtoolkit.controller;

import com.devtoolkit.model.ToolResponse;
import com.devtoolkit.service.JwtService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/jwt")
@RequiredArgsConstructor
@Validated
@CrossOrigin(origins = "http://localhost:4200")
public class JwtController {

    private final JwtService jwtService;

    /**
     * POST /api/v1/jwt/decode
     * Body: { "token": "eyJ..." }
     */
    @PostMapping("/decode")
    public ResponseEntity<ToolResponse<Map<String, Object>>> decode(
            @RequestBody Map<String, @NotBlank String> body) {

        String token = body.get("token");
        if (token == null || token.isBlank()) {
            return ResponseEntity.badRequest()
                .body(ToolResponse.error("Le champ 'token' est requis"));
        }

        return ResponseEntity.ok(jwtService.decode(token));
    }

    /**
     * POST /api/v1/jwt/explain
     * Body: { "payload": "{...json payload...}" }
     * Appelle Spring AI pour analyser les claims en langage naturel.
     */
    @PostMapping("/explain")
    public ResponseEntity<ToolResponse<Map<String, Object>>> explain(
            @RequestBody Map<String, String> body) {

        String payload = body.get("payload");
        if (payload == null || payload.isBlank()) {
            return ResponseEntity.badRequest()
                .body(ToolResponse.error("Le champ 'payload' est requis"));
        }

        // Limite la taille envoyée à l'IA
        if (payload.length() > 5000) {
            return ResponseEntity.badRequest()
                .body(ToolResponse.error("Payload trop grand (max 5000 caractères)"));
        }

        ToolResponse<Map<String,Object>> response=jwtService.explainWithAI(payload);
        log.info("Response: "+response);

        return ResponseEntity.ok(response);
    }
}
