package com.zynboot.jitu;

import com.zynboot.jitu.config.JituGeoProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication(scanBasePackages = "com.zynboot")
@EnableConfigurationProperties(JituGeoProperties.class)
public class ZynJituApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZynJituApplication.class, args);
    }
}
