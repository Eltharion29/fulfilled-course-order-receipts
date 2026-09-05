package dev.learningstore.receipts.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

@Validated
@ConfigurationProperties(prefix = "infrai")
public record InfraiProperties(
        @NotBlank String baseUrl,
        @NotBlank String apiKey,
        @Min(1) int maxAttempts
) {
}
