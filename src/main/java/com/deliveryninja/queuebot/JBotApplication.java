package com.deliveryninja.queuebot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"me.ramswaroop.jbot", "com.deliveryninja.queuebot"})
public class JBotApplication {

    public static void main(String[] args) {
        SpringApplication.run(JBotApplication.class, args);
    }

}
