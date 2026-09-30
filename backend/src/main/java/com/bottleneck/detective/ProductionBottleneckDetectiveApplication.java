package com.bottleneck.detective;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Production Bottleneck Detective application.
 *
 * @SpringBootApplication enables:
 *   - @Configuration  : marks this as a source of bean definitions
 *   - @EnableAutoConfiguration : Spring Boot auto-configures based on classpath
 *   - @ComponentScan  : scans all sub-packages for beans
 */
@SpringBootApplication
public class ProductionBottleneckDetectiveApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProductionBottleneckDetectiveApplication.class, args);
    }
}
