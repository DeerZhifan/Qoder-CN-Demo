package com.cdp.kb;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.cdp.kb.mapper")
public class CdpKbApplication {
    public static void main(String[] args) {
        SpringApplication.run(CdpKbApplication.class, args);
    }
}
