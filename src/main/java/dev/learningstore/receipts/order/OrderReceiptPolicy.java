package dev.learningstore.receipts.order;

import org.springframework.stereotype.Component;

@Component
public class OrderReceiptPolicy {
    public ReceiptDecision decide(CourseOrder order) {
        if (order.fulfillmentStatus() == CourseOrder.FulfillmentStatus.FULFILLED) {
            return new ReceiptDecision(true, true, "RECEIPT_READY");
        }
        return new ReceiptDecision(false, false, "AWAITING_FULFILLMENT");
    }
}
