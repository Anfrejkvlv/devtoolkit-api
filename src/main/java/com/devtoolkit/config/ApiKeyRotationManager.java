package com.devtoolkit.config;

import com.devtoolkit.exception.NoAvailableApiKeyException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Component
@Slf4j
public class ApiKeyRotationManager {

    private final List<GeminiApiKey> apiKeys;
    private final AtomicInteger currentKeyIndex = new AtomicInteger(0);

    public ApiKeyRotationManager(GeminiProperties props) {
        this.apiKeys = props.getApiKeys()
                .stream()
                .map(GeminiApiKey::new)
                .collect(Collectors.toCollection(ArrayList::new));

        log.info("{} clé(s) Gemini API chargée(s)", apiKeys.size());
    }

    /**
     * Retourne la clé courante si disponible, sinon tourne vers la suivante
     */
    public GeminiApiKey getAvailableKey() {
        int attempts = 0;

        while (attempts < apiKeys.size()) {
            int index = currentKeyIndex.get() % apiKeys.size();
            GeminiApiKey key = apiKeys.get(index);

            if (key.isAvailable()) {
                log.debug("Clé API active: index={}", index);
                return key;
            }

            log.warn("Clé {} épuisée, rotation vers la suivante...", index);
            rotateToNextKey();
            attempts++;
        }

        throw new NoAvailableApiKeyException(
                "Toutes les clés Gemini API sont temporairement épuisées"
        );
    }

    public void markCurrentKeyExhausted() {
        int index = currentKeyIndex.get() % apiKeys.size();
        GeminiApiKey key = apiKeys.get(index);
        log.warn("Clé API {} marquée comme épuisée", index);
        key.markExhausted();
        rotateToNextKey();
    }

    private void rotateToNextKey() {
        currentKeyIndex.incrementAndGet();
    }

    public int getAvailableKeyCount() {
        return (int) apiKeys.stream().filter(GeminiApiKey::isAvailable).count();
    }
}
