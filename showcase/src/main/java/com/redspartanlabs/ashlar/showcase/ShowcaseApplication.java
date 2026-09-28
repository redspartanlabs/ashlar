package com.redspartanlabs.ashlar.showcase;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * An ordinary Spring Boot application that happens to consume Ashlar. It
 * exists to prove that consumption works through the published artifact, so
 * it is deliberately plain - nothing here is part of the library.
 */
@SpringBootApplication
public class ShowcaseApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShowcaseApplication.class, args);
    }
}
