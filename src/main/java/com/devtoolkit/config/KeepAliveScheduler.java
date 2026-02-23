package com.devtoolkit.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class KeepAliveScheduler {

    private final WebClient webClient;

    @Value("${app.api-url}")
    private String apiUrl;

    public KeepAliveScheduler(WebClient.Builder builder) {
        this.webClient = builder.build();
    }

    @Scheduled(fixedRate = 55, timeUnit = TimeUnit.MINUTES)
    public void keepAlive() {
        webClient.get()
                .uri(apiUrl + "/actuator/health")
                .retrieve()
                .toBodilessEntity()
                .subscribe(
                        response -> log.debug("Keep-Alive OK:{}",response.getStatusCode()),
                        error -> log.error("Keep-Alive Error:{}", error.getMessage())
                );
    }
}
