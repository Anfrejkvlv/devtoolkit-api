package com.devtoolkit.config;

import com.devtoolkit.exception.IncompleteResponseException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
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
//    public <T> T execute(Function<ChatClient, T> action) {
//        while (true) {
//            String model = fallbackManager.getAvailableModel();
//            log.info("Exécution avec le modèle: {}", model);
//
//            ChatClient client = chatClientBuilder
//                    .defaultOptions(GoogleGenAiChatOptions.builder()
//                            .model(model)
//                            .build())
//                    .build();
//            try {
//                return action.apply(client);
//            } catch (Exception e) {
//                log.info("LOG: {}", e.getMessage());
//                if (fallbackManager.isRetryableError(e)) {
//                    fallbackManager.markAsExhausted(model);
//                    log.warn("Rate limit sur {}, tentative avec le modèle suivant...", model);
//                    // continue le while → réessaie avec le prochain modèle
//                } else {
//                    throw new RuntimeException("Erreur inattendue avec le modèle " + model, e);
//                }
//            }
//        }
//    }

    public <T> T execute(Function<ChatClient.ChatClientRequestSpec, T> action) {
        while (true) {
            String model = fallbackManager.getAvailableModel();
            log.info("Exécution avec le modèle: {}", model);

            ChatClient client = chatClientBuilder
                    .defaultOptions(GoogleGenAiChatOptions.builder()
                            .model(model)
                            .build())
                    .build();
            try {
                // On force le passage par ChatResponse pour vérifier le finishReason
                ChatClient.ChatClientRequestSpec spec = client.prompt();
                ChatResponse chatResponse = (ChatResponse) action.apply(spec); // voir plus bas

                // Vérifier si la réponse est complète
                checkResponseCompleteness(chatResponse, model);

                return (T) chatResponse.getResult().getOutput().getText();

            } catch (IncompleteResponseException e) {
                fallbackManager.markAsExhausted(model);
                log.warn("Réponse incomplète sur {}, tentative avec modèle suivant...", model);
            } catch (Exception e) {
                log.info("Exception: {}", e.getMessage());
                if (fallbackManager.isRetryableError(e)) {
                    fallbackManager.markAsExhausted(model);
                    log.warn("Rate limit sur {}, tentative avec modèle suivant...", model);
                } else {
                    throw new RuntimeException("Erreur inattendue avec le modèle " + model, e);
                }
            }
        }
    }

    // Pour récupérer juste le texte (avec vérification)
    public String executeForText(Function<ChatClient, ChatResponse> action) {
        while (true) {
            String model = fallbackManager.getAvailableModel();
            ChatClient client = chatClientBuilder
                    .defaultOptions(GoogleGenAiChatOptions.builder().model(model).build())
                    .build();
            try {
                ChatResponse response = action.apply(client);
                checkResponseCompleteness(response, model);
                return response.getResult().getOutput().getText();

            } catch (IncompleteResponseException e) {
                fallbackManager.markAsExhausted(model);
                log.warn("Réponse incomplète sur {}, fallback...", model);
            } catch (Exception e) {
                if (fallbackManager.isRetryableError(e)) {
                    fallbackManager.markAsExhausted(model);
                } else {
                    throw new RuntimeException("Erreur inattendue avec " + model, e);
                }
            }
        }
    }


    private void checkResponseCompleteness(ChatResponse response, String model) {
        if (response == null || response.getResult() == null) {
            throw new IncompleteResponseException("Réponse null du modèle " + model);
        }

        String finishReason = response.getResult()
                .getMetadata()
                .getFinishReason();

        log.info("FinishReason pour {}: {}", model, finishReason);

        // MAX_TOKENS = réponse coupée = on doit changer de modèle
        if ("MAX_TOKENS".equalsIgnoreCase(finishReason)
                || "LENGTH".equalsIgnoreCase(finishReason)) {
            log.warn("Réponse tronquée sur {} (finishReason={})", model, finishReason);
            throw new IncompleteResponseException("Réponse tronquée: " + finishReason);
        }
    }
}
