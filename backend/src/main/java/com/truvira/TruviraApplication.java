package com.truvira;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ENTRY POINT of the whole backend. Like main() in any Java program.
 *
 * @SpringBootApplication = 3 annotations combined:
 *   1. @Configuration         -> this class can define beans (objects Spring manages)
 *   2. @EnableAutoConfiguration -> Spring looks at our dependencies (web, JPA, MySQL...) and
 *                                  configures them automatically (starts Tomcat, connects DB...)
 *   3. @ComponentScan         -> Spring scans THIS package (com.truvira) and sub-packages for
 *                                classes marked @RestController, @Service, @Repository, etc.
 *                                and creates objects of them automatically.
 */
@SpringBootApplication
public class TruviraApplication {

    public static void main(String[] args) {
        // Starts Spring, scans our classes, starts the web server on port 8080.
        SpringApplication.run(TruviraApplication.class, args);
    }
}
