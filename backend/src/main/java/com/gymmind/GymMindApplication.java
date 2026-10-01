package com.gymmind;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SpringBootApplication
@EnableScheduling
public class GymMindApplication {

    private static final Logger log = LoggerFactory.getLogger(GymMindApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(GymMindApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("========== GymMind 后端启动成功，已准备好接收请求 ==========");
    }
}
