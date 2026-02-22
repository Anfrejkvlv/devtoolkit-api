package com.devtoolkit.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.Duration;
import java.util.List;

/**
 * Configuration du cache — 3 modes selon l'environnement :
 *
 *  CACHE_TYPE=caffeine  → Cache Caffeine en mémoire (Cloud Run $0, défaut)
 *  CACHE_TYPE=redis     → Délégué à Spring auto-config (Render + Redis)
 *  CACHE_TYPE=none      → Spring désactive le cache automatiquement
 *
 * Caffeine vs Redis :
 *  + Caffeine : gratuit, zéro latence réseau, aucune dépendance externe
 *  - Caffeine : local au conteneur (perdu au redémarrage / scale-out)
 *  + Redis    : cache partagé entre instances, persistant
 *  - Redis    : service externe (payant sauf Upstash free 10K cmd/jour)
 */
@Configuration
@Slf4j
public class CacheConfig {

    @Value("${spring.cache.type:caffeine}")
    private String cacheType;

    private static final long TTL_DEFAULT = 3_600;   // 1h
    private static final int  MAX_ENTRIES = 500;

    @Bean
    @Primary
    public CacheManager cacheManager() {
        if ("caffeine".equalsIgnoreCase(cacheType)) {
            log.info("✅ Cache : Caffeine (memoire locale) — mode $0 Cloud Run");
            CaffeineCacheManager manager = new CaffeineCacheManager();
            manager.setAllowNullValues(false);
            manager.setCaffeine(
                Caffeine.newBuilder()
                    .maximumSize(MAX_ENTRIES)
                    .expireAfterWrite(Duration.ofSeconds(TTL_DEFAULT))
                    .recordStats()
            );
            manager.setCacheNames(List.of(
                "jwt-decode","jwt-ai-explain",
                "cron-generate", "cron-explain",
                "sql-optimize", "sql-explain",
                "spring-props-generate", "spring-props-review"
            ));
            return manager;
        }

        // Fallback : laisse Spring auto-configurer Redis ou simple map
        log.warn("⚠️  Cache type '{}' — Spring auto-configuration activee", cacheType);
        CaffeineCacheManager fallback = new CaffeineCacheManager();
        fallback.setAllowNullValues(false);
        fallback.setCaffeine(Caffeine.newBuilder().maximumSize(200)
            .expireAfterWrite(Duration.ofSeconds(TTL_DEFAULT)));
        return fallback;
    }
}
