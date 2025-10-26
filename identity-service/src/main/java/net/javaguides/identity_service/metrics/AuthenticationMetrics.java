package net.javaguides.identity_service.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationMetrics {

    private final MeterRegistry meterRegistry;

    public AuthenticationMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordAuthenticationOutcome(boolean isSuccessful, String reason) {
        String outcome = isSuccessful ? "success" : "failure";
        String failureReason = isSuccessful ? "N/A" : reason;

        meterRegistry.counter("identity_auth_total",
                "job", "identity-service",
                "outcome", outcome,
                "failureReason", failureReason
        ).increment();
    }
}