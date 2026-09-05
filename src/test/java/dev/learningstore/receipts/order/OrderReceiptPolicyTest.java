package dev.learningstore.receipts.order;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

class OrderReceiptPolicyTest {
    private final OrderReceiptPolicy policy = new OrderReceiptPolicy();

    @Test
    void fulfilledCourseOrderProducesAnImmutableCustomerReceipt() {
        CourseOrder order = order(CourseOrder.FulfillmentStatus.FULFILLED);

        ReceiptDecision decision = policy.decide(order);

        assertThat(decision.issueReceipt()).isTrue();
        assertThat(decision.flatten()).isTrue();
        assertThat(decision.customerUpdate()).isEqualTo("RECEIPT_READY");
    }

    @Test
    void paidOrderWaitsUntilCourseAccessIsFulfilled() {
        CourseOrder order = order(CourseOrder.FulfillmentStatus.PAID);

        ReceiptDecision decision = policy.decide(order);

        assertThat(decision.issueReceipt()).isFalse();
        assertThat(decision.customerUpdate()).isEqualTo("AWAITING_FULFILLMENT");
    }

    private CourseOrder order(CourseOrder.FulfillmentStatus status) {
        return new CourseOrder(
                "course-1042",
                "Mina Chen",
                "mina@example.com",
                "Practical Geometry",
                new BigDecimal("49.00"),
                status,
                URI.create("https://example.com/course-receipt.pdf")
        );
    }
}
