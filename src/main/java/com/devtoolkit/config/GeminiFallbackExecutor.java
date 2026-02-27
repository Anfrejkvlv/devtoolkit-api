package com.devtoolkit.config;

import com.devtoolkit.exception.IncompleteResponseException;
import com.devtoolkit.exception.NoAvailableModelException;
import com.google.genai.Client;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Slf4j
public class GeminiFallbackExecutor {

    // Map clé API → ChatClient prêt à l'emploi
    private final Map<String, ChatClient> clientsByApiKey;
    private final ModelFallbackManager modelFallbackManager;
    private final ApiKeyRotationManager apiKeyRotationManager;

    public GeminiFallbackExecutor(GeminiProperties props,
                                  ModelFallbackManager modelFallbackManager,
                                  ApiKeyRotationManager apiKeyRotationManager) {
        this.modelFallbackManager = modelFallbackManager;
        this.apiKeyRotationManager = apiKeyRotationManager;

        // Préconstruction au démarrage
        this.clientsByApiKey = props.getApiKeys().stream()
                .collect(Collectors.toMap(
                        key -> key,
                        key -> {
                            Client genAiClient= Client.builder()
                                    .apiKey(key)
                                    .build();

                            GoogleGenAiChatModel chatModel=GoogleGenAiChatModel.builder()
                                    .genAiClient(genAiClient)
                                    .build();

                            return ChatClient.create(chatModel);
                        }));
        log.info("{} ChatClient(s) Gemini initialisés", clientsByApiKey.size());
    }

    public String executeForText(Function<ChatClient, ChatResponse> action) {
        while (true) {
            GeminiApiKey apiKey = apiKeyRotationManager.getAvailableKey();
            ChatClient client = clientsByApiKey.get(apiKey.getKey());
            modelFallbackManager.resetExhaustedModels();

            while (true) {
                String model = null;
                try {
                    model = modelFallbackManager.getAvailableModel();
                    log.info("→ clé: {}, modèle: {}", maskKey(apiKey.getKey()), model);

                    // Injecter le modèle au moment de l'appel
                    ChatResponse response = action.apply(
                            client.mutate()
                                    .defaultOptions(GoogleGenAiChatOptions.builder()
                                            .model(model)
                                            .build())
                                    .build()
                    );

                    checkResponseCompleteness(response, model);
                    return response.getResult().getOutput().getText();

                } catch (IncompleteResponseException e) {
                    modelFallbackManager.markAsExhausted(model);

                } catch (NoAvailableModelException e) {
                    log.warn("Modèles épuisés → rotation clé API");
                    apiKeyRotationManager.markCurrentKeyExhausted();
                    break;

                } catch (Exception e) {
                    if (modelFallbackManager.isRetryableError(e)) {
                        modelFallbackManager.markAsExhausted(model);
                    } else if (isApiKeyError(e)) {
                        apiKeyRotationManager.markCurrentKeyExhausted();
                        break;
                    } else {
                        throw new RuntimeException("Erreur inattendue: " + model, e);
                    }
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

    private boolean isApiKeyError(Exception e) {
        Throwable cause = e;
        while (cause != null) {
            String msg = cause.getMessage() != null ? cause.getMessage().toLowerCase() : "";
            if (msg.contains("401") || msg.contains("403")
                    || msg.contains("api key") || msg.contains("quota")
                    || msg.contains("billing")) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    private String maskKey(String key) {
        if (key == null || key.length() < 8) return "***";
        return key.substring(0, 4) + "..." + key.substring(key.length() - 4);
    }

}
