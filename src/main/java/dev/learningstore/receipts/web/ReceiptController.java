package dev.learningstore.receipts.web;

import dev.learningstore.receipts.gateway.InfraiException;
import dev.learningstore.receipts.order.CourseOrder;
import dev.learningstore.receipts.order.FulfilledOrderReceiptService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/orders")
public class ReceiptController {
    private final FulfilledOrderReceiptService receipts;

    public ReceiptController(FulfilledOrderReceiptService receipts) {
        this.receipts = receipts;
    }

    @PostMapping("/receipt")
    public FulfilledOrderReceiptService.ReceiptResult createReceipt(@Valid @RequestBody CourseOrder order) {
        return receipts.process(order);
    }

    @ExceptionHandler(InfraiException.class)
    public ResponseEntity<Map<String, Object>> rejected(InfraiException error) {
        HttpStatus status = error.status() >= 400 && error.status() < 500
                ? HttpStatus.valueOf(error.status())
                : HttpStatus.BAD_GATEWAY;
        return ResponseEntity.status(status).body(Map.of(
                "message", error.getMessage(),
                "details", error.details()
        ));
    }
}
