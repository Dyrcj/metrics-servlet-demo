package com.yeepay.example;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.servlets.MetricsServlet;

/**
 * Created by yp-tc-m-7163 on 2017/3/28.
 *
 */
public class MetricsServletContextListener extends MetricsServlet.ContextListener {
    private final MetricRegistry registry;
    public MetricsServletContextListener(MetricRegistry registry) {
        this.registry = registry;
    }
    protected MetricRegistry getMetricRegistry() {
        return registry;
    }
}
