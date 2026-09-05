package dev.learningstore.receipts.order;

public record ReceiptDecision(boolean issueReceipt, boolean flatten, String customerUpdate) {
}
