package com.kb.manager;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.kb.manager.mapper")
public class KbManagerApplication {

    public static void main(String[] args) {
        SpringApplication.run(KbManagerApplication.class, args);
    }
}
