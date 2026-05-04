package com.web3.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.autoconfigure.ConfigurationCustomizer;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置类
 * 
 * 核心功能：
 * 1. ✅ 分页插件配置
 * 2. ✅ 乐观锁插件配置
 * 
 * 注意：自动填充功能已移至 com.web3.config.MyMetaObjectHandler
 */
@Configuration
public class MyBatisPlusConfig {
    
    /**
     * 配置 MyBatis-Plus 拦截器
     * 
     * @return MybatisPlusInterceptor
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        
        // ✅ 分页插件（支持 MySQL）
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        
        // ✅ 乐观锁插件（配合 @Version 注解使用）
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        
        return interceptor;
    }
    
    /**
     * 配置自动填充处理器
     * 
     * @return ConfigurationCustomizer
     */
    @Bean
    public ConfigurationCustomizer configurationCustomizer() {
        return configuration -> {
            // MyBatis-Plus 3.5.x 版本不再需要手动注册 MetaObjectHandler
            // Spring Boot 会自动扫描并注册实现了 MetaObjectHandler 接口的 Bean
            // 参见: com.web3.config.MyMetaObjectHandler
        };
    }
}
