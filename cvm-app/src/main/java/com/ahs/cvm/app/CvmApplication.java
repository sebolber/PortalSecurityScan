package com.ahs.cvm.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.ahs.cvm")
public class CvmApplication {

    public static void main(String[] args) {
        SpringApplication.run(CvmApplication.class, args);
    }
}
