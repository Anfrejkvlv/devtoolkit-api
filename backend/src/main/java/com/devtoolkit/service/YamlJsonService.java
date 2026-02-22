package com.devtoolkit.service;

import com.devtoolkit.model.ToolResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@Slf4j
public class YamlJsonService {

    // ObjectMapper JSON standard (pretty print)
    private final ObjectMapper jsonMapper = new ObjectMapper()
        .enable(SerializationFeature.INDENT_OUTPUT);

    // ObjectMapper YAML (sans le --- en tête de document)
    private final ObjectMapper yamlMapper = new ObjectMapper(
        YAMLFactory.builder()
            .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)  // pas de "---"
            .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)          // moins de guillemets inutiles
            .build()
    );

    /**
     * JSON → YAML
     */
    public ToolResponse<Map<String, Object>> jsonToYaml(String json) {
        try {
            // 1. Parser le JSON
            Object parsed = jsonMapper.readValue(json.trim(), Object.class);
            // 2. Sérialiser en YAML
            String yaml = yamlMapper.writeValueAsString(parsed);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("output",      yaml);
            result.put("direction",   "json→yaml");
            result.put("inputLines",  json.lines().count());
            result.put("outputLines", yaml.lines().count());

            return ToolResponse.ok(result);

        } catch (Exception e) {
            log.warn("Erreur JSON→YAML : {}", e.getMessage());
            return ToolResponse.error("JSON invalide : " + e.getMessage());
        }
    }

    /**
     * YAML → JSON
     */
    public ToolResponse<Map<String, Object>> yamlToJson(String yaml) {
        try {
            // 1. Parser le YAML
            Object parsed = yamlMapper.readValue(yaml.trim(), Object.class);
            // 2. Sérialiser en JSON pretty
            String json = jsonMapper.writeValueAsString(parsed);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("output",      json);
            result.put("direction",   "yaml→json");
            result.put("inputLines",  yaml.lines().count());
            result.put("outputLines", json.lines().count());

            return ToolResponse.ok(result);

        } catch (Exception e) {
            log.warn("Erreur YAML→JSON : {}", e.getMessage());
            return ToolResponse.error("YAML invalide : " + e.getMessage());
        }
    }

    /**
     * Validation YAML pure (sans conversion)
     */
    public ToolResponse<Map<String, Object>> validate(String input, String format) {
        try {
            ObjectMapper mapper = format.equalsIgnoreCase("yaml") ? yamlMapper : jsonMapper;
            mapper.readValue(input.trim(), Object.class);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("valid",  true);
            result.put("format", format);
            result.put("lines",  input.lines().count());
            result.put("chars",  input.length());

            return ToolResponse.ok(result);

        } catch (Exception e) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("valid",  false);
            result.put("format", format);
            result.put("error",  e.getMessage());

            return ToolResponse.ok(result); // 200 avec valid=false
        }
    }
}
