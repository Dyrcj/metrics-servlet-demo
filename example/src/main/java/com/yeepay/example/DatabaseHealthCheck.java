package com.yeepay.example;

import com.codahale.metrics.health.HealthCheck;
import com.codahale.metrics.health.HealthCheckRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * Created by yp-tc-m-7163 on 2017/3/28.
 *
 */
@Component
public class DatabaseHealthCheck extends HealthCheck {

    public static final String HEALTHCHECK_NAME = "databaseConnection";

    @Autowired
    private Database database;

    @Autowired
    private HealthCheckRegistry healthCheckRegistry;

    @PostConstruct
    public void addToRegistry() {
        healthCheckRegistry.register(HEALTHCHECK_NAME, this);
    }

    protected Result check() throws Exception {
        if (database.isConnected()) {
            return HealthCheck.Result.healthy();
        } else {
            return HealthCheck.Result.unhealthy("Cannot connect to " + database.getUrl());
        }
    }
}
