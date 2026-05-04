package com.web3.chain;

import com.web3.config.Web3Properties;
import com.web3.entity.ChainConfig;
import com.web3.service.ChainConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthBlockNumber;
import org.web3j.protocol.http.HttpService;

import javax.annotation.PostConstruct;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 多链管理器 - 管理多个 Web3j 实例
 * 支持动态加载链配置，无需重启服务
 */
@Slf4j
@Component
public class MultiChainManager {

    // 使用 ConcurrentHashMap 支持运行时动态添加
    private final Map<String, Web3j> web3jMap = new ConcurrentHashMap<>();

    @Autowired
    private Web3Properties web3Properties;

    @Autowired
    private ChainConfigService chainConfigService;

    /**
     * 获取指定链的 Web3j 实例
     */
    public Web3j getWeb3j(String chainName) {
        Web3j web3j = web3jMap.get(chainName);
        if (web3j == null) {
            throw new IllegalArgumentException("链 " + chainName + " 未配置或已禁用");
        }
        return web3j;
    }

    /**
     * 获取所有启用的链配置
     */
    public List<ChainConfig> getActiveChains() {
        return chainConfigService.getActiveChains();
    }

    /**
     * 获取当前区块号
     */
    public BigInteger getCurrentBlock(String chainName) {
        try {
            Web3j web3j = getWeb3j(chainName);
            EthBlockNumber blockNumber = web3j.ethBlockNumber().send();
            return blockNumber.getBlockNumber();
        } catch (Exception e) {
            log.error("获取链 {} 的区块号失败", chainName, e);
            throw new RuntimeException("获取区块号失败", e);
        }
    }

    /**
     * 初始化时创建所有链的 Web3j 实例
     */
    @PostConstruct
    public void init() {
        log.info("Web3j 多链管理器初始化完成");
        
        // 从数据库读取链配置
        List<ChainConfig> chains = chainConfigService.getActiveChains();
        
        for (ChainConfig chain : chains) {
            loadChain(chain);
        }
        
        log.info("已配置的链数量: {}", web3jMap.size());
    }

    /**
     * 动态加载单个链配置（无需重启服务）
     * 
     * @param chainConfig 链配置
     * @return 是否加载成功
     */
    public boolean loadChain(ChainConfig chainConfig) {
        String chainName = chainConfig.getChainName();
        
        // 避免重复加载
        if (web3jMap.containsKey(chainName)) {
            log.warn("链 {} 已加载，跳过", chainName);
            return false;
        }
        
        try {
            log.info("动态加载链配置 - 链: {}, RPC: {}", chainName, chainConfig.getRpcUrl());
            Web3j web3j = Web3j.build(new HttpService(chainConfig.getRpcUrl()));
            web3jMap.put(chainName, web3j);
            
            // 验证连接
            BigInteger blockNumber = web3j.ethBlockNumber().send().getBlockNumber();
            log.info("链 {} 加载成功 - 当前区块: {}", chainName, blockNumber);
            
            return true;
        } catch (Exception e) {
            log.error("链 {} 加载失败", chainName, e);
            return false;
        }
    }

    /**
     * 卸载指定链（清理 Web3j 实例）
     * 
     * @param chainName 链名称
     * @return 是否卸载成功
     */
    public boolean unloadChain(String chainName) {
        Web3j web3j = web3jMap.remove(chainName);
        if (web3j != null) {
            try {
                web3j.shutdown();
                log.info("链 {} 已卸载", chainName);
                return true;
            } catch (Exception e) {
                log.error("卸载链 {} 时发生异常", chainName, e);
                return false;
            }
        } else {
            log.warn("链 {} 未加载，无需卸载", chainName);
            return false;
        }
    }

    /**
     * 重新加载所有链配置（用于批量更新）
     * 
     * @return 成功加载的链数量
     */
    public int reloadAllChains() {
        log.info("开始重新加载所有链配置");
        
        // 先卸载所有链
        List<String> currentChains = List.copyOf(web3jMap.keySet());
        currentChains.forEach(this::unloadChain);
        
        // 重新加载
        List<ChainConfig> chains = chainConfigService.getActiveChains();
        int successCount = 0;
        for (ChainConfig chain : chains) {
            if (loadChain(chain)) {
                successCount++;
            }
        }
        
        log.info("重新加载完成 - 成功: {}/{}", successCount, chains.size());
        return successCount;
    }
}
