package com.progolf.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The Spring Boot application entry point (spec: world-session). Component scanning is rooted at
 * {@code com.progolf.app}, so the framework-free simulation engine ({@code com.progolf.sim}) is never
 * turned into Spring beans — the application wraps the engine; the engine never depends on the application.
 */
@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
