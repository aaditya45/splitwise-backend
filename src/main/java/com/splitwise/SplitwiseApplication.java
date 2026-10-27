package com.splitwise;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SplitwiseApplication {

    public static void main(String[] args) {
        loadDotenvForLocalDevelopment();
        SpringApplication.run(SplitwiseApplication.class, args);
    }

    /**
     * A .env file is a local-development convenience. It is optional, and it never overrides a
     * value that is already set as a real environment variable or JVM property, so deployed
     * environments (containers, cloud) are always configured by their own environment.
     */
    private static void loadDotenvForLocalDevelopment() {
        Dotenv dotenv = Dotenv.configure()
                .ignoreIfMissing()
                .ignoreIfMalformed()
                .load();

        dotenv.entries(Dotenv.Filter.DECLARED_IN_ENV_FILE).forEach(entry -> {
            String key = entry.getKey();
            if (System.getenv(key) == null && System.getProperty(key) == null) {
                System.setProperty(key, entry.getValue());
            }
        });
    }
}
