package io.github.shad3n.predicatemapper.examples.sender;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Query sender application that automatically executes queries on boot,
 * logs results to console, then shuts down.
 */
@SpringBootApplication
public class QuerySenderApplication {

    public static void main(String[] args) {
        SpringApplication.run(QuerySenderApplication.class, args);
    }
}