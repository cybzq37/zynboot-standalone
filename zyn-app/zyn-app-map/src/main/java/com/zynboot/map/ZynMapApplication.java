package com.zynboot.map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.zynboot")
@EnableAsync
@EnableScheduling
public class ZynMapApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZynMapApplication.class, args);
    }
}
