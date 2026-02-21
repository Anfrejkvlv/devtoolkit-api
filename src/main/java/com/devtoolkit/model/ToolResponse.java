package com.devtoolkit.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

/**
 * Enveloppe générique pour toutes les réponses API.
 * Garantit un format cohérent côté Angular.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ToolResponse<T> {

    private boolean success;
    private T data;
    private String error;
    private boolean aiEnhanced;   // true si la réponse contient une analyse IA

    public static <T> ToolResponse<T> ok(T data) {
        return ToolResponse.<T>builder()
            .success(true)
            .data(data)
            .build();
    }

    public static <T> ToolResponse<T> okWithAI(T data) {
        return ToolResponse.<T>builder()
            .success(true)
            .data(data)
            .aiEnhanced(true)
            .build();
    }

    public static <T> ToolResponse<T> error(String message) {
        return ToolResponse.<T>builder()
            .success(false)
            .error(message)
            .build();
    }
}
