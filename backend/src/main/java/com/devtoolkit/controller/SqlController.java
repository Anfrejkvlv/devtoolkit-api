package com.devtoolkit.controller;

import com.devtoolkit.model.ToolResponse;
import com.devtoolkit.service.SqlService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/sql")
@RequiredArgsConstructor
public class SqlController {

    private final SqlService sqlService;

    /**
     * POST /api/v1/sql/format
     * Body: { "sql": "SELECT ...", "dialect": "postgresql" }
     */
    @PostMapping("/format")
    public ResponseEntity<ToolResponse<Map<String, Object>>> format(
            @RequestBody Map<String, String> body) {

        String sql     = body.getOrDefault("sql", "").trim();
        String dialect = body.getOrDefault("dialect", "standard");

        if (sql.isBlank()) {
            return ResponseEntity.badRequest()
                .body(ToolResponse.error("Le champ 'sql' est requis"));
        }

        return ResponseEntity.ok(sqlService.format(sql, dialect));
    }

    /**
     * POST /api/v1/sql/optimize
     * Body: { "sql": "SELECT ...", "dialect": "postgresql" }
     * → Spring AI analyse et propose indexes + réécriture
     */
    @PostMapping("/optimize")
    public ResponseEntity<ToolResponse<Map<String, Object>>> optimize(
            @RequestBody Map<String, String> body) {

        String sql     = body.getOrDefault("sql", "").trim();
        String dialect = body.getOrDefault("dialect", "standard");

        if (sql.isBlank()) {
            return ResponseEntity.badRequest()
                .body(ToolResponse.error("Le champ 'sql' est requis"));
        }

        return ResponseEntity.ok(sqlService.optimize(sql, dialect));
    }

    /**
     * POST /api/v1/sql/explain
     * Body: { "sql": "SELECT ..." }
     * → Spring AI explique en langage naturel
     */
    @PostMapping("/explain")
    public ResponseEntity<ToolResponse<Map<String, Object>>> explain(
            @RequestBody Map<String, String> body) {

        String sql = body.getOrDefault("sql", "").trim();

        if (sql.isBlank()) {
            return ResponseEntity.badRequest()
                .body(ToolResponse.error("Le champ 'sql' est requis"));
        }

        return ResponseEntity.ok(sqlService.explain(sql));
    }
}
