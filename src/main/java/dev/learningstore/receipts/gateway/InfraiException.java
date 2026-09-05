package dev.learningstore.receipts.gateway;

import java.util.Map;

public class InfraiException extends RuntimeException {
    private final int status;
    private final Map<String, Object> details;

    public InfraiException(int status, Map<String, Object> details) {
        super(String.valueOf(details.getOrDefault("message", "Infrai request rejected")));
        this.status = status;
        this.details = Map.copyOf(details);
    }

    public int status() {
        return status;
    }

    public Map<String, Object> details() {
        return details;
    }
}
