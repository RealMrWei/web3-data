package com.web3.config;

import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.redis.spring.RedisLockProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Redis 配置类 - 包含分布式锁支持
 */
@Slf4j
@Configuration
public class RedisConfig {

    /**
     * StringRedisTemplate Bean
     */
    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }

    /**
     * ShedLock 分布式锁提供者
     */
    @Bean
    public LockProvider lockProvider(RedisConnectionFactory connectionFactory) {
        log.info("初始化 Redis 分布式锁提供者");
        return new RedisLockProvider(connectionFactory);
    }
}
