package com.zynboot.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.zynboot")
public class ZynDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZynDemoApplication.class, args);
    }
}
