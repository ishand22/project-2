package com.entitleflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class EntitleFlowApplication {
    public static void main(String[] args) { SpringApplication.run(EntitleFlowApplication.class, args); }
}
