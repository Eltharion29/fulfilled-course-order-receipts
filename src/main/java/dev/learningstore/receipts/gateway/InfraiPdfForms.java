package dev.learningstore.receipts.gateway;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.learningstore.receipts.config.InfraiProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class InfraiPdfForms {
    private static final String PDF_GENERATE_PATH = "/v1/pdf/generate";

    private final InfraiProperties properties;
    private final ObjectMapper json;
    private final HttpClient http;

    @Autowired
    public InfraiPdfForms(InfraiProperties properties, ObjectMapper json) {
        this(properties, json, HttpClient.newHttpClient());
    }

    InfraiPdfForms(InfraiProperties properties, ObjectMapper json, HttpClient http) {
        this.properties = properties;
        this.json = json;
        this.http = http;
    }

    public Map<String, Object> fillAndFlatten(
            URI templatePdf,
            Map<String, String> fields,
            String idempotencyKey
    ) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("template_html", "<html><body>"
                + "<h1>Course receipt</h1>"
                + "<p>Order: {{order_id}}</p>"
                + "<p>Learner: {{learner_name}}</p>"
                + "<p>Email: {{learner_email}}</p>"
                + "<p>Course: {{course_title}}</p>"
                + "<p>Amount paid: {{amount_paid}}</p>"
                + "<p>Status: {{fulfillment_status}}</p>"
                + "</body></html>");
        body.put("template_vars", fields);
        body.put("idempotency_key", idempotencyKey);

        for (int attempt = 1; attempt <= properties.maxAttempts(); attempt++) {
            HttpRequest request = request(body, idempotencyKey);
            HttpResponse<String> response = send(request);
            Envelope envelope = decode(response.body());

            if (response.statusCode() == 429 && attempt < properties.maxAttempts()) {
                pause(retryDelay(response, attempt));
                continue;
            }
            if (!envelope.ok()) {
                throw new InfraiException(response.statusCode(), envelope.error());
            }
            if (response.statusCode() >= 500) {
                throw new IllegalStateException("Infrai transport response: " + response.statusCode());
            }
            return envelope.data();
        }
        throw new IllegalStateException("Retry attempts exhausted");
    }

    private HttpRequest request(Map<String, Object> body, String idempotencyKey) {
        try {
            return HttpRequest.newBuilder(URI.create(properties.baseUrl() + PDF_GENERATE_PATH))
                    .header("Authorization", "Bearer " + properties.apiKey())
                    .header("Content-Type", "application/json")
                    .header("Idempotency-Key", idempotencyKey)
                    .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)))
                    .build();
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not encode receipt request", e);
        }
    }

    private HttpResponse<String> send(HttpRequest request) {
        try {
            return http.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new IllegalStateException("Could not reach Infrai", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Receipt request interrupted", e);
        }
    }

    private Envelope decode(String body) {
        try {
            return json.readValue(body, new TypeReference<Envelope>() { });
        } catch (IOException e) {
            throw new IllegalStateException("Could not decode Infrai envelope", e);
        }
    }

    private Duration retryDelay(HttpResponse<?> response, int attempt) {
        return response.headers().firstValue("Retry-After")
                .map(value -> Duration.ofSeconds(Long.parseLong(value)))
                .orElseGet(() -> Duration.ofMillis(250L * (1L << (attempt - 1))));
    }

    private void pause(Duration delay) {
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Receipt retry interrupted", e);
        }
    }

    private record Envelope(
            boolean ok,
            Map<String, Object> data,
            Map<String, Object> error,
            Map<String, Object> metadata
    ) {
        private Envelope {
            data = data == null ? Map.of() : Map.copyOf(data);
            error = error == null ? Map.of() : Map.copyOf(error);
            metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        }
    }
}
