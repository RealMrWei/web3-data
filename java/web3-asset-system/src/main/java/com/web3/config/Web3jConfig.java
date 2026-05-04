package com.web3.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

import java.util.HashMap;
import java.util.Map;

/**
 * Web3j 配置类 - 支持多链
 */
@Slf4j
@Configuration
public class Web3jConfig {

    @Autowired
    private Web3Properties web3Properties;

    /**
     * 创建多个 Web3j 实例（每个链一个）
     */
    @Bean
    public Map<String, Web3j> web3jMap() {
        Map<String, Web3j> web3jMap = new HashMap<>();

        for (Web3Properties.ChainConfig chain : web3Properties.getChains()) {
            if (Boolean.TRUE.equals(chain.getEnabled())) {
                log.info("初始化 Web3j 实例 - 链: {}, RPC: {}", chain.getName(), chain.getRpcUrl());
                Web3j web3j = Web3j.build(new HttpService(chain.getRpcUrl()));
                web3jMap.put(chain.getName(), web3j);
            }
        }

        return web3jMap;
    }

    /**
     * 创建默认 Web3j 实例（使用第一个启用的链）
     */
    @Bean("defaultWeb3j")
    public Web3j defaultWeb3j() {
        // 获取第一个启用的链配置
        Web3Properties.ChainConfig firstChain = web3Properties.getChains().stream()
                .filter(chain -> Boolean.TRUE.equals(chain.getEnabled()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("未找到启用的链配置"));

        log.info("创建默认 Web3j 实例 - 链: {}, RPC: {}", firstChain.getName(), firstChain.getRpcUrl());
        return Web3j.build(new HttpService(firstChain.getRpcUrl()));
    }

    /**
     * 创建 Credentials（用于交易签名）
     */
    @Bean
    public Credentials credentials() {
        String privateKey = web3Properties.getPrivateKey();
        if (privateKey == null || privateKey.isEmpty()) {
            log.warn("未配置私钥，将使用默认测试账户");
            // 这里应该从安全的地方加载私钥
            return null;
        }
        return Credentials.create(privateKey);
    }
}
