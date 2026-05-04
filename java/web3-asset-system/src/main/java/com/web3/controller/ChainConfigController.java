package com.web3.controller;

import com.web3.chain.MultiChainManager;
import com.web3.entity.ChainConfig;
import com.web3.service.ChainConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 链配置管理控制器
 * 支持动态加载链配置，无需重启服务
 */
@Slf4j
@RestController
@RequestMapping("/api/chain")
public class ChainConfigController {

    @Autowired
    private ChainConfigService chainConfigService;

    @Autowired
    private MultiChainManager multiChainManager;

    /**
     * 查询所有启用的链配置
     */
    @GetMapping("/list")
    public ResponseEntity<List<ChainConfig>> listActiveChains() {
        log.info("查询所有启用的链配置");
        List<ChainConfig> chains = chainConfigService.getActiveChains();
        return ResponseEntity.ok(chains);
    }

    /**
     * 根据链名称查询配置
     */
    @GetMapping("/{chainName}")
    public ResponseEntity<ChainConfig> getByChainName(@PathVariable String chainName) {
        log.info("查询链配置: {}", chainName);
        ChainConfig config = chainConfigService.getByChainName(chainName);
        return ResponseEntity.ok(config);
    }

    /**
     * 添加新链配置并动态加载（无需重启服务）
     */
    @PostMapping("/add")
    public ResponseEntity<String> addChain(@RequestBody ChainConfig chainConfig) {
        log.info("添加新链配置: {}", chainConfig.getChainName());
        
        // 1. 保存到数据库
        boolean dbSuccess = chainConfigService.addChain(chainConfig);
        if (!dbSuccess) {
            return ResponseEntity.badRequest().body("链配置保存失败");
        }
        
        // 2. 动态加载到运行中的服务
        boolean loadSuccess = multiChainManager.loadChain(chainConfig);
        
        if (loadSuccess) {
            return ResponseEntity.ok("链配置添加并加载成功");
        } else {
            return ResponseEntity.status(207).body("链配置已保存到数据库，但加载到运行时失败（请检查 RPC 连接）");
        }
    }

    /**
     * 更新链配置
     */
    @PutMapping("/update")
    public ResponseEntity<String> updateChain(@RequestBody ChainConfig chainConfig) {
        log.info("更新链配置: {}", chainConfig.getChainName());
        boolean success = chainConfigService.updateChain(chainConfig);
        if (success) {
            // TODO: 可以选择重新加载该链的 Web3j 实例
            return ResponseEntity.ok("链配置更新成功");
        } else {
            return ResponseEntity.badRequest().body("链配置更新失败");
        }
    }

    /**
     * 禁用链配置并卸载
     */
    @PutMapping("/disable/{chainName}")
    public ResponseEntity<String> disableChain(@PathVariable String chainName) {
        log.info("禁用链配置: {}", chainName);
        
        // 1. 更新数据库状态
        boolean dbSuccess = chainConfigService.disableChain(chainName);
        
        // 2. 从运行中卸载
        boolean unloadSuccess = multiChainManager.unloadChain(chainName);
        
        if (dbSuccess && unloadSuccess) {
            return ResponseEntity.ok("链配置已禁用并卸载");
        } else {
            return ResponseEntity.status(207).body("链配置已禁用，但卸载运行时实例失败");
        }
    }

    /**
     * 启用链配置并动态加载
     */
    @PutMapping("/enable/{chainName}")
    public ResponseEntity<String> enableChain(@PathVariable String chainName) {
        log.info("启用链配置: {}", chainName);
        
        // 1. 启用数据库记录
        boolean dbSuccess = chainConfigService.enableChain(chainName);
        if (!dbSuccess) {
            return ResponseEntity.badRequest().body("链配置启用失败");
        }
        
        // 2. 动态加载
        ChainConfig config = chainConfigService.getByChainName(chainName);
        boolean loadSuccess = multiChainManager.loadChain(config);
        
        if (loadSuccess) {
            return ResponseEntity.ok("链配置已启用并加载");
        } else {
            return ResponseEntity.status(207).body("链配置已启用，但加载到运行时失败");
        }
    }

    /**
     * 重新加载所有链配置
     */
    @PostMapping("/reload")
    public ResponseEntity<String> reloadAllChains() {
        log.info("重新加载所有链配置");
        int successCount = multiChainManager.reloadAllChains();
        return ResponseEntity.ok("重新加载完成，成功加载 " + successCount + " 个链");
    }
}
