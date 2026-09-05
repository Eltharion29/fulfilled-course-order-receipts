package dev.learningstore.receipts;

import dev.learningstore.receipts.config.InfraiProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(InfraiProperties.class)
public class ReceiptApplication {
    public static void main(String[] args) {
        SpringApplication.run(ReceiptApplication.class, args);
    }
}
