package dev.genesshoan.fitnesstrackerapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class FitnessTrackerApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(FitnessTrackerApiApplication.class, args);
    }
}
