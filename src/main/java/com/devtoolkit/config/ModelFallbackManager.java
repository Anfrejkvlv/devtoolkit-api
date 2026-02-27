package com.devtoolkit.config;

import com.devtoolkit.exception.NoAvailableModelException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class ModelFallbackManager {

    private final List<String> models;
    @Getter
    private final Map<String, Instant> exhaustedModels= new ConcurrentHashMap<>();

    // Durée avant de réessayer un autre model
    private static final Duration COOLDOWN_DURATION = Duration.ofSeconds(55);

    public ModelFallbackManager(GeminiProperties props) {
        this.models = new ArrayList<>(props.getModels());
    }

    /**
     * Retourne le prochain modèle disponible
     * @return
     */
    public String getAvailableModel() {
        Instant now = Instant.now();

        return models.stream()
                .filter(model->{
                    Instant exhaustedAt = exhaustedModels.get(model);

                    if (exhaustedAt == null) return true;

                    if (now.isAfter(exhaustedAt.plus(COOLDOWN_DURATION))) {
                        exhaustedModels.remove(model);
                        log.info("Modèle reactivé après cooldown {}", model);
                        return true;
                    }
                    return false;
                })
                .findFirst()
                .orElseThrow(()->new NoAvailableModelException("Tous les modèles Gemini sont temporairement épuisés"));
    }

    /**
     * Marquer un modèle comme épuisé (rate limit atteint)
     */
    public void markAsExhausted(String model) {
        log.info("Modèle {} marque comme épuise (rate limit)", model);

        exhaustedModels.put(model, Instant.now());
        // Ajout en dernier pour la priorité
        models.remove(model);
        models.add(model);
    }

    public boolean isRateLimitError(Exception e) {
        String message=e.getMessage()!=null?e.getMessage().toUpperCase():"";

        return message.contains("429")
                || message.contains("rate limit")
                || message.contains("quota exceeded")
                || message.contains("resource exhausted")
                || message.contains("503");
    }

    public boolean isRetryableError(Exception e) {
        Throwable cause = e;
        while (cause != null) {
            String msg = cause.getMessage() != null ? cause.getMessage().toLowerCase() : "";
            // Erreurs qui valent la peine de changer de modèle
            if (msg.contains("429") || msg.contains("503")
                    || msg.contains("rate limit") || msg.contains("quota exceeded")
                    || msg.contains("resource exhausted") || msg.contains("high demand")
                    || msg.contains("try again later") || msg.contains("overloaded")) {
                return true;
            }
            // Erreurs définitives → ne pas retenter (clé invalide, prompt bloqué...)
            if (msg.contains("400") || msg.contains("401")
                    || msg.contains("403") || msg.contains("invalid")) {
                return false;
            }
            cause = cause.getCause();
        }
        return false;
    }

    public void resetExhaustedModels() {
        exhaustedModels.clear();
    }

}
