package com.taja.crm.crm_backend.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Duration;
import lombok.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "app.rate-limit") @Validated @Getter @Setter
public class RateLimitProperties {
    private boolean enabled = true;
    @Min(1) @Max(1000000) private int maxKeys = 20000;
    @Valid @NotNull private Limit loginIp = new Limit(10, Duration.ofMinutes(1));
    @Valid @NotNull private Limit loginAccount = new Limit(5, Duration.ofMinutes(1));
    @Valid @NotNull private Limit forgotIp = new Limit(5, Duration.ofMinutes(15));
    @Valid @NotNull private Limit forgotEmail = new Limit(1, Duration.ofMinutes(1));
    @Valid @NotNull private Limit importUser = new Limit(5, Duration.ofMinutes(1));
    @Valid @NotNull private Limit generalIp = new Limit(120, Duration.ofMinutes(1));
    @Valid @NotNull private Limit generalUser = new Limit(300, Duration.ofMinutes(1));
    @Valid @NotNull private Limit authIp = new Limit(60, Duration.ofMinutes(1));

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class Limit {
        @Min(1) @Max(1000000) private int capacity;
        @NotNull private Duration period;
        @AssertTrue(message = "period 必須大於 0 且不超過 30 天")
        public boolean isValidPeriod() { return period != null && !period.isNegative() && !period.isZero() && period.compareTo(Duration.ofDays(30)) <= 0; }
    }
}
