package com.example.demo331bacnkend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Application entry point.
 * We scan both 'bacnkend' and 'backnend' packages to tolerate the typo
 * without touching existing working code elsewhere.
 */
@SpringBootApplication
@ComponentScan(basePackages = {
        "com.example.demo331bacnkend", // current package (typo kept)
        "com.example.demo331backnend"  // other package spelling
})
@EntityScan(basePackages = {
        "com.example.demo331bacnkend",
        "com.example.demo331backnend"
})
@EnableJpaRepositories(basePackages = {
        "com.example.demo331bacnkend",
        "com.example.demo331backnend"
})
public class Demo331BacnkendApplication {

    public static void main(String[] args) {
        SpringApplication.run(Demo331BacnkendApplication.class, args);
    }
}
