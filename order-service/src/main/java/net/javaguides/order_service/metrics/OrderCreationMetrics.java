package net.javaguides.order_service.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class OrderCreationMetrics {

    private final MeterRegistry meterRegistry;

    public OrderCreationMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordAuthenticationOutcome(boolean isSuccessful, String reason) {
        String outcome = isSuccessful ? "success" : "failure";
        String failureReason = isSuccessful ? "N/A" : reason;

        meterRegistry.counter("order_creation_total",
                "job", "order-service",
                "outcome", outcome,
                "failureReason", failureReason
        ).increment();
    }

    public void recordCancellationOutcome(boolean isSuccessful, String failureType) {
        String outcome = isSuccessful ? "success" : "failure";
        String reason = isSuccessful ? "N/A" : failureType;

        meterRegistry.counter("order_cancellation_total",
                "job", "order-service",
                "outcome", outcome,
                "reason", reason
        ).increment();
    }
}