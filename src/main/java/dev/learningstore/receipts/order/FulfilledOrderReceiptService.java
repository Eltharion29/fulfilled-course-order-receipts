package dev.learningstore.receipts.order;

import dev.learningstore.receipts.gateway.InfraiPdfForms;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class FulfilledOrderReceiptService {
    private final OrderReceiptPolicy policy;
    private final InfraiPdfForms pdfForms;

    public FulfilledOrderReceiptService(OrderReceiptPolicy policy, InfraiPdfForms pdfForms) {
        this.policy = policy;
        this.pdfForms = pdfForms;
    }

    public ReceiptResult process(CourseOrder order) {
        ReceiptDecision decision = policy.decide(order);
        if (!decision.issueReceipt()) {
            return new ReceiptResult(order.orderId(), decision.customerUpdate(), Map.of());
        }

        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("order_id", order.orderId());
        fields.put("learner_name", order.learnerName());
        fields.put("learner_email", order.learnerEmail());
        fields.put("course_title", order.courseTitle());
        fields.put("amount_paid", order.amountPaid().toPlainString());
        fields.put("fulfillment_status", order.fulfillmentStatus().name());

        Map<String, Object> receipt = pdfForms.fillAndFlatten(
                order.receiptTemplatePdf(),
                fields,
                "course-order-receipt-" + order.orderId()
        );
        return new ReceiptResult(order.orderId(), decision.customerUpdate(), receipt);
    }

    public record ReceiptResult(String orderId, String customerUpdate, Map<String, Object> receipt) {
    }
}
