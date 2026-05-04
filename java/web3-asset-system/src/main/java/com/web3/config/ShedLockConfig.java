package com.web3.config;

import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.context.annotation.Configuration;

/**
 * ShedLock 配置类 - 启用定时任务分布式锁
 */
@Configuration
@EnableSchedulerLock(defaultLockAtMostFor = "10m")
public class ShedLockConfig {
    // 锁配置已通过 RedisConfig 中的 LockProvider Bean 实现
    // 此处仅启用 @EnableSchedulerLock 注解
}
