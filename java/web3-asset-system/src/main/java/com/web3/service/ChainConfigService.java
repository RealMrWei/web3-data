package com.web3.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.web3.entity.ChainConfig;
import com.web3.mapper.ChainConfigMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * 链配置服务 - 从数据库管理多链配置
 */
@Slf4j
@Service
public class ChainConfigService {

    @Autowired
    private ChainConfigMapper chainConfigMapper;

    /**
     * 初始化日志
     */
    @PostConstruct
    public void init() {
        log.info("链配置服务初始化完成");
    }

    /**
     * 获取所有启用的链配置（带缓存）
     */
    @Cacheable(value = "chainConfig", key = "'activeChains'")
    public List<ChainConfig> getActiveChains() {
        LambdaQueryWrapper<ChainConfig> query = new LambdaQueryWrapper<>();
        query.eq(ChainConfig::getStatus, 1)
             .orderByAsc(ChainConfig::getChainName);
        
        List<ChainConfig> chains = chainConfigMapper.selectList(query);
        log.info("获取到 {} 个启用的链配置", chains.size());
        return chains;
    }

    /**
     * 根据链名称获取配置
     */
    @Cacheable(value = "chainConfig", key = "#chainName")
    public ChainConfig getByChainName(String chainName) {
        LambdaQueryWrapper<ChainConfig> query = new LambdaQueryWrapper<>();
        query.eq(ChainConfig::getChainName, chainName);
        
        ChainConfig config = chainConfigMapper.selectOne(query);
        if (config == null) {
            throw new IllegalArgumentException("链配置不存在: " + chainName);
        }
        return config;
    }

    /**
     * 添加新链配置
     */
    @CacheEvict(value = "chainConfig", allEntries = true)
    public boolean addChain(ChainConfig chainConfig) {
        int result = chainConfigMapper.insert(chainConfig);
        log.info("添加新链配置: {}", chainConfig.getChainName());
        return result > 0;
    }

    /**
     * 更新链配置
     */
    @CacheEvict(value = "chainConfig", allEntries = true)
    public boolean updateChain(ChainConfig chainConfig) {
        int result = chainConfigMapper.updateById(chainConfig);
        log.info("更新链配置: {}", chainConfig.getChainName());
        return result > 0;
    }

    /**
     * 禁用链配置
     */
    @CacheEvict(value = "chainConfig", allEntries = true)
    public boolean disableChain(String chainName) {
        ChainConfig config = getByChainName(chainName);
        config.setStatus(0);
        return updateChain(config);
    }

    /**
     * 启用链配置
     */
    @CacheEvict(value = "chainConfig", allEntries = true)
    public boolean enableChain(String chainName) {
        ChainConfig config = getByChainName(chainName);
        config.setStatus(1);
        return updateChain(config);
    }
}
