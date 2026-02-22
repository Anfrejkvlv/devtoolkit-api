package com.devtoolkit.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.stereotype.Component;

import java.util.function.Function;

@Component
@Slf4j
public class GeminiFallbackExecutor {

    private final ChatClient.Builder chatClientBuilder;
    private final ModelFallbackManager fallbackManager;

    public GeminiFallbackExecutor(ChatClient.Builder chatClientBuilder,
                                  ModelFallbackManager fallbackManager) {
        this.chatClientBuilder = chatClientBuilder;
        this.fallbackManager = fallbackManager;
    }

    /**
     * Exécute une fonction qui reçoit un ChatClient configuré avec le bon modèle,
     * et bascule automatiquement en cas de rate limit.
     */
    public <T> T execute(Function<ChatClient, T> action) {
        while (true) {
            String model = fallbackManager.getAvailableModel();
            log.info("Exécution avec le modèle: {}", model);

            ChatClient client = chatClientBuilder
                    .defaultOptions(GoogleGenAiChatOptions.builder()
                            .model(model)
                            .build())
                    .build();
            try {
                return action.apply(client);
            } catch (Exception e) {
                if (fallbackManager.isRetryableError(e)) {
                    fallbackManager.markAsExhausted(model);
                    log.warn("Rate limit sur {}, tentative avec le modèle suivant...", model);
                    // continue le while → réessaie avec le prochain modèle
                } else {
                    throw new RuntimeException("Erreur inattendue avec le modèle " + model, e);
                }
            }
        }
    }
}
