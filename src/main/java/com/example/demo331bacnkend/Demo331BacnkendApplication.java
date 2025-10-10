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
        "com.example.demo331backnend",  // other package spelling
        "se331.lab.rest.security"       // security package
})
@EntityScan(basePackages = {
        "com.example.demo331bacnkend.entity",
        "com.example.demo331backnend.entity",
        "se331.lab.rest.security.user",
        "se331.lab.rest.security.token"
})
@EnableJpaRepositories(basePackages = {
        "com.example.demo331bacnkend.repository",
        "com.example.demo331backnend.repository",
        "se331.lab.rest.security.user",
        "se331.lab.rest.security.token"
})
public class Demo331BacnkendApplication {

    public static void main(String[] args) {
        SpringApplication.run(Demo331BacnkendApplication.class, args);
    }
}
