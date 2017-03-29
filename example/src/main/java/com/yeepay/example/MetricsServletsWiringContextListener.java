package com.yeepay.example;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheckRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * Created by yp-tc-m-7163 on 2017/3/28.
 *
 */
public class MetricsServletsWiringContextListener implements ServletContextListener {
    @Autowired
    private MetricRegistry metricRegistry;

    @Autowired
    private HealthCheckRegistry healthCheckRegistry;

    private MetricsServletContextListener metricsServletContextListener;
    private HealthCheckServletContextListener healthCheckServletContextListener;
    public void contextInitialized(ServletContextEvent event) {
        WebApplicationContextUtils.getRequiredWebApplicationContext(event.getServletContext())
                .getAutowireCapableBeanFactory()
                .autowireBean(this);

        metricsServletContextListener = new MetricsServletContextListener(metricRegistry);
        healthCheckServletContextListener = new HealthCheckServletContextListener(healthCheckRegistry);

        metricsServletContextListener.contextInitialized(event);
        healthCheckServletContextListener.contextInitialized(event);
    }

    public void contextDestroyed(ServletContextEvent event) {

    }
}
