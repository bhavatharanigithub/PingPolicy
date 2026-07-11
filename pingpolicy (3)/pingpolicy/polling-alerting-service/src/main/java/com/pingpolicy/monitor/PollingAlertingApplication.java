package com.pingpolicy.monitor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PollingAlertingApplication {

    public static void main(String[] args) {
        SpringApplication.run(PollingAlertingApplication.class, args);
    }
}
