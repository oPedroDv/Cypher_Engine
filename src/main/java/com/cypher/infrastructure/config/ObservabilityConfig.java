package com.cypher.infrastructure.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Slf4j
@Configuration
public class ObservabilityConfig {

    @Value("${spring.application.name:cypher-risk-engine}")
    private String applicationName;

    @Value("${cypher.environment:local}")
    private String environment;

    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> registry.config()
                .commonTags(List.of(
                        Tag.of("application", applicationName),
                        Tag.of("environment", environment)
                ));
    }

    @Bean
    public JvmMemoryMetrics jvmMemoryMetrics() { return new JvmMemoryMetrics(); }

    @Bean
    public JvmGcMetrics jvmGcMetrics() { return new JvmGcMetrics(); }

    @Bean
    public JvmThreadMetrics jvmThreadMetrics() { return new JvmThreadMetrics(); }

    @Bean
    public ProcessorMetrics processorMetrics() { return new ProcessorMetrics(); }
}