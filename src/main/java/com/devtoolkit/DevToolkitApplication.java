package com.devtoolkit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class DevToolkitApplication {
    public static void main(String[] args) {
        SpringApplication.run(DevToolkitApplication.class, args);
    }
}
