package com.web3.controller;

import com.web3.entity.ContractAddress;
import com.web3.service.ContractAddressService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 合约地址管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/contract")
public class ContractAddressController {

    @Autowired
    private ContractAddressService contractAddressService;

    /**
     * 查询所有启用的合约地址
     */
    @GetMapping("/list")
    public ResponseEntity<List<ContractAddress>> listAllContracts() {
        log.info("查询所有启用的合约地址");
        List<ContractAddress> contracts = contractAddressService.getAllActive();
        return ResponseEntity.ok(contracts);
    }

    /**
     * 根据链名称查询所有合约地址
     */
    @GetMapping("/list/{chainName}")
    public ResponseEntity<List<ContractAddress>> listByChainName(@PathVariable String chainName) {
        log.info("查询链 {} 的所有合约地址", chainName);
        List<ContractAddress> contracts = contractAddressService.getByChainName(chainName);
        return ResponseEntity.ok(contracts);
    }

    /**
     * 根据链名称和合约类型查询合约地址
     */
    @GetMapping("/{chainName}/{contractType}")
    public ResponseEntity<String> getContractAddress(
            @PathVariable String chainName,
            @PathVariable String contractType) {
        log.info("查询合约地址 - 链: {}, 类型: {}", chainName, contractType);
        String address = contractAddressService.getContractAddress(chainName, contractType);
        return ResponseEntity.ok(address);
    }

    /**
     * 添加新的合约地址配置
     */
    @PostMapping("/add")
    public ResponseEntity<String> addContract(@RequestBody ContractAddress contractAddress) {
        log.info("添加合约地址 - 链: {}, 类型: {}", 
                contractAddress.getChainName(), 
                contractAddress.getContractType());
        
        boolean success = contractAddressService.addContractAddress(contractAddress);
        if (success) {
            return ResponseEntity.ok("合约地址添加成功");
        } else {
            return ResponseEntity.badRequest().body("合约地址添加失败");
        }
    }

    /**
     * 更新合约地址配置
     */
    @PutMapping("/update")
    public ResponseEntity<String> updateContract(@RequestBody ContractAddress contractAddress) {
        log.info("更新合约地址 - 链: {}, 类型: {}", 
                contractAddress.getChainName(), 
                contractAddress.getContractType());
        
        boolean success = contractAddressService.updateContractAddress(contractAddress);
        if (success) {
            return ResponseEntity.ok("合约地址更新成功");
        } else {
            return ResponseEntity.badRequest().body("合约地址更新失败");
        }
    }

    /**
     * 禁用合约地址
     */
    @PutMapping("/disable/{chainName}/{contractType}")
    public ResponseEntity<String> disableContract(
            @PathVariable String chainName,
            @PathVariable String contractType) {
        log.info("禁用合约地址 - 链: {}, 类型: {}", chainName, contractType);
        
        boolean success = contractAddressService.disableContract(chainName, contractType);
        if (success) {
            return ResponseEntity.ok("合约地址已禁用");
        } else {
            return ResponseEntity.badRequest().body("合约地址禁用失败");
        }
    }

    /**
     * 批量添加合约地址（用于新链部署）
     */
    @PostMapping("/batch-add")
    public ResponseEntity<String> batchAddContracts(@RequestBody List<ContractAddress> contracts) {
        log.info("批量添加合约地址，数量: {}", contracts.size());
        
        int successCount = 0;
        int failCount = 0;
        
        for (ContractAddress contract : contracts) {
            try {
                if (contractAddressService.addContractAddress(contract)) {
                    successCount++;
                } else {
                    failCount++;
                }
            } catch (Exception e) {
                log.error("添加合约地址失败: {}", contract, e);
                failCount++;
            }
        }
        
        String result = String.format("批量添加完成 - 成功: %d, 失败: %d", successCount, failCount);
        return ResponseEntity.ok(result);
    }
}
