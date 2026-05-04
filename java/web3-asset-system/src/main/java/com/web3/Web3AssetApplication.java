package com.web3;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Web3 链上资产管理系统主启动类
 */
@SpringBootApplication
@EnableScheduling
public class Web3AssetApplication {

    public static void main(String[] args) {
        SpringApplication.run(Web3AssetApplication.class, args);
    }
}
