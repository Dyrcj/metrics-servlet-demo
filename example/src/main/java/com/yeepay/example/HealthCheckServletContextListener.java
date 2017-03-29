package com.yeepay.example;

import com.codahale.metrics.health.HealthCheckRegistry;
import com.codahale.metrics.servlets.HealthCheckServlet;

import java.util.concurrent.ExecutorService;

/**
 * Created by yp-tc-m-7163 on 2017/3/28.
 *
 */
public class HealthCheckServletContextListener extends HealthCheckServlet.ContextListener {
    private final HealthCheckRegistry registry;

    public HealthCheckServletContextListener(HealthCheckRegistry metricRegistry) {
        this.registry = metricRegistry;
    }

    protected ExecutorService getExecutorService() {
        // don't use a thread pool by default
        return null;
    }

    protected HealthCheckRegistry getHealthCheckRegistry() {
        return registry;
    }
}
