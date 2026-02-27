package com.devtoolkit.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@ConfigurationProperties(prefix = "gemini")
@Configuration
@Getter
@Setter
public class GeminiProperties {
    private List<String> models;
    private List<String> apiKeys;
}
