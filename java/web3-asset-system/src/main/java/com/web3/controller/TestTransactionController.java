package com.web3.controller;

import com.web3.service.Web3TransactionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 测试用 Controller - 用于触发链上交易
 * 注意：仅用于开发测试，生产环境应移除此 Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/test")
public class TestTransactionController {
    
    @Autowired
    private Web3TransactionService transactionService;
    
    /**
     * 测试充值（调用合约 deposit 方法）
     * 
     * @param tokenAddress 代币合约地址
     * @param amount 充值金额
     * @return 交易结果
     */
    @PostMapping("/deposit")
    public ResponseEntity<Map<String, Object>> testDeposit(
            @RequestParam String tokenAddress,
            @RequestParam BigDecimal amount) {
        
        log.info("测试充值: token={}, amount={}", tokenAddress, amount);
        
        try {
            String txHash = transactionService.deposit(tokenAddress, amount);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "充值交易已发送");
            response.put("txHash", txHash);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("充值失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("message", "充值失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * 测试提现（调用合约 requestWithdrawal 方法）
     * 
     * @param tokenAddress 代币合约地址
     * @param amount 提现金额
     * @return 交易结果
     */
    @PostMapping("/withdrawal")
    public ResponseEntity<Map<String, Object>> testWithdrawal(
            @RequestParam String tokenAddress,
            @RequestParam BigDecimal amount) {
        
        log.info("测试提现: token={}, amount={}", tokenAddress, amount);
        
        try {
            String txHash = transactionService.requestWithdrawal(tokenAddress, amount);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "提现请求已发送");
            response.put("txHash", txHash);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("提现失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("message", "提现失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}