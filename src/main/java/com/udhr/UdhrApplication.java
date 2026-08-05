package com.udhr;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class UdhrApplication {

    public static void main(String[] args) {
        SpringApplication.run(UdhrApplication.class, args);
    }
}
