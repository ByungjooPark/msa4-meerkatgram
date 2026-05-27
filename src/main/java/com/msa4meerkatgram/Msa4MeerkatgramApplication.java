package com.msa4meerkatgram;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class Msa4MeerkatgramApplication {

    public static void main(String[] args) {
        SpringApplication.run(Msa4MeerkatgramApplication.class, args);
    }

}
