package dev.learningstore.receipts.order;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.net.URI;

public record CourseOrder(
        @NotBlank String orderId,
        @NotBlank String learnerName,
        @Email @NotBlank String learnerEmail,
        @NotBlank String courseTitle,
        @NotNull @Positive BigDecimal amountPaid,
        @NotNull FulfillmentStatus fulfillmentStatus,
        @NotNull URI receiptTemplatePdf
) {
    public enum FulfillmentStatus {
        CHECKOUT_PENDING,
        PAID,
        FULFILLED
    }
}
