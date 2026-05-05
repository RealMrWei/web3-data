package com.web3.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.web3.entity.DepositRecord;
import com.web3.entity.WithdrawalOrder;
import com.web3.mapper.DepositRecordMapper;
import com.web3.mapper.UserAssetMapper;
import com.web3.mapper.WithdrawalOrderMapper;
import com.web3.service.AssetManagementService;
import com.web3.service.DepositService;
import com.web3.service.WithdrawalService;
import com.web3.utils.Web3ContractClient;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 资产管理 REST API 控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/assets")
public class AssetController {

    @Autowired
    private AssetManagementService assetManagementService;

    @Autowired
    private DepositRecordMapper depositRecordMapper;

    @Autowired
    private WithdrawalOrderMapper withdrawalOrderMapper;

    /**
     * 查询用户总资产（多链）
     */
    @GetMapping("/total-balance")
    public Map<String, Object> getTotalBalance(@RequestParam String userAddress) {
        try {
            Map<String, BigInteger> balances = assetManagementService.getTotalBalance(userAddress);

            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("data", balances);
            return result;
        } catch (Exception e) {
            log.error("查询总资产失败", e);
            Map<String, Object> result = new HashMap<>();
            result.put("code", 500);
            result.put("message", "查询失败: " + e.getMessage());
            return result;
        }
    }

    /**
     * 查询用户在指定链的余额
     */
    @GetMapping("/balance")
    public Map<String, Object> getBalance(
            @RequestParam String chainName,
            @RequestParam String userAddress) {

        try {
            BigInteger balance = assetManagementService.getBalanceOnChain(chainName, userAddress);

            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("data", balance.toString());
            return result;
        } catch (Exception e) {
            log.error("查询余额失败", e);
            Map<String, Object> result = new HashMap<>();
            result.put("code", 500);
            result.put("message", "查询失败: " + e.getMessage());
            return result;
        }
    }

    /**
     * 原生币充值
     */
    @PostMapping("/deposit/native")
    public Map<String, Object> depositNative(@RequestBody NativeDepositRequest request) {
        try {
            String txHash = assetManagementService.depositNative(
                    request.getChainName(),
                    request.getFromAddress(),
                    request.getPrivateKey(),
                    new BigInteger(request.getAmount()));

            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("message", "充值成功");
            result.put("txHash", txHash);
            return result;
        } catch (Exception e) {
            log.error("原生币充值失败", e);
            Map<String, Object> result = new HashMap<>();
            result.put("code", 500);
            result.put("message", "充值失败: " + e.getMessage());
            return result;
        }
    }

    /**
     * ERC20代币充值
     */
    @PostMapping("/deposit/erc20")
    public Map<String, Object> depositERC20(@RequestBody ERC20DepositRequest request) {
        try {
            String txHash = assetManagementService.depositERC20(
                    request.getChainName(),
                    request.getTokenAddress(),
                    request.getFromAddress(),
                    request.getPrivateKey(),
                    new BigInteger(request.getAmount()));

            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("message", "充值成功");
            result.put("txHash", txHash);
            return result;
        } catch (Exception e) {
            log.error("ERC20充值失败", e);
            Map<String, Object> result = new HashMap<>();
            result.put("code", 500);
            result.put("message", "充值失败: " + e.getMessage());
            return result;
        }
    }

    /**
     * 查询充值记录
     */
    @GetMapping("/deposits")
    public Map<String, Object> getDeposits(
            @RequestParam(required = false) String chainName,
            @RequestParam(required = false) String userAddress,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {

        Page<DepositRecord> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<DepositRecord> wrapper = new LambdaQueryWrapper<>();

        if (chainName != null && !chainName.isEmpty()) {
            wrapper.eq(DepositRecord::getChainName, chainName);
        }
        if (userAddress != null && !userAddress.isEmpty()) {
            wrapper.eq(DepositRecord::getUserAddress, userAddress);
        }

        wrapper.orderByDesc(DepositRecord::getCreateTime);

        Page<DepositRecord> resultPage = depositRecordMapper.selectPage(page, wrapper);

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", resultPage.getRecords());
        result.put("total", resultPage.getTotal());
        result.put("pageNum", resultPage.getCurrent());
        result.put("pageSize", resultPage.getSize());

        return result;
    }

    /**
     * 查询提现订单
     */
    @GetMapping("/withdrawals")
    public Map<String, Object> getWithdrawals(
            @RequestParam(required = false) String userAddress,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {

        Page<WithdrawalOrder> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<WithdrawalOrder> wrapper = new LambdaQueryWrapper<>();

        if (userAddress != null && !userAddress.isEmpty()) {
            wrapper.eq(WithdrawalOrder::getUserAddress, userAddress);
        }

        wrapper.orderByDesc(WithdrawalOrder::getCreateTime);

        Page<WithdrawalOrder> resultPage = withdrawalOrderMapper.selectPage(page, wrapper);

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", resultPage.getRecords());
        result.put("total", resultPage.getTotal());

        return result;
    }

    @PostMapping("/deposit/erc20with-client")
    public Map<String, Object> depositWithClient(@RequestBody ERC20DepositRequest request) {
        try {
            String txHash = assetManagementService.depositWithClient(
                    request.getChainName(),
                    request.getTokenAddress(),
                    request.getFromAddress(),
                    request.getPrivateKey(),
                    new BigInteger(request.getAmount()));

            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("message", "充值成功");
            result.put("txHash", txHash);
            return result;
        } catch (Exception e) {
            log.error("ERC20充值失败", e);
            Map<String, Object> result = new HashMap<>();
            result.put("code", 500);
            result.put("message", "充值失败: " + e.getMessage());
            return result;
        }
    }

    // ==================== DTO Classes ====================

    @Data
    static class NativeDepositRequest {
        private String chainName;
        private String fromAddress;
        private String privateKey;
        private String amount;
    }

    @Data
    static class ERC20DepositRequest {
        private String chainName;
        private String tokenAddress;
        private String fromAddress;
        private String privateKey;
        private String amount;
    }

    @Data
    static class WithdrawalRequest {
        private String chainName;
        private String userAddress;
        private String recipient;
        private String amount;
        private String tokenAddress;
    }
}
