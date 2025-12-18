package ru.domium.documentservice.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {

  @Bean
  Counter documentsGeneratedTotal(MeterRegistry reg) {
    return Counter.builder("documents_generated_total").register(reg);
  }

  @Bean
  Counter documentsSignedTotal(MeterRegistry reg) {
    return Counter.builder("documents_signed_total").register(reg);
  }

  @Bean
  Counter documentsRejectedTotal(MeterRegistry reg) {
    return Counter.builder("documents_rejected_total").register(reg);
  }
}
