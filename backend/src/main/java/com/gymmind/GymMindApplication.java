package com.gymmind;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GymMindApplication {

    public static void main(String[] args) {
        SpringApplication.run(GymMindApplication.class, args);
    }
}
