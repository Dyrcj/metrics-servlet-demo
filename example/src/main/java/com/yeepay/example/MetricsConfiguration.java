package com.yeepay.example;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheckRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Created by yp-tc-m-7163 on 2017/3/28.
 *
 */
@Configuration
public class MetricsConfiguration {
    @Bean
    public HealthCheckRegistry newHealthCheckRegistry() {
        return new HealthCheckRegistry();
    }

    @Bean
    public MetricRegistry newMetricRegistry() {
        return new MetricRegistry();
    }
}
