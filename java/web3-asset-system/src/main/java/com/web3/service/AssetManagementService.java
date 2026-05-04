package com.web3.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

/**
 * 统一资产管理服务 - 整合充值、提现、查询等功能
 * 提供对外的高层次API
 */
@Slf4j
@Service
public class AssetManagementService {

    private final DepositService depositService;
    private final WithdrawalService withdrawalService;
    private final AssetQueryService assetQueryService;
    private final EventListenerService eventListenerService;

    public AssetManagementService(DepositService depositService,
                                  WithdrawalService withdrawalService,
                                  AssetQueryService assetQueryService,
                                  EventListenerService eventListenerService) {
        this.depositService = depositService;
        this.withdrawalService = withdrawalService;
        this.assetQueryService = assetQueryService;
        this.eventListenerService = eventListenerService;
    }

    // ==================== 充值相关 ====================

    /**
     * 原生币充值
     */
    public String depositNative(String chainName, String fromAddress, String privateKey, BigInteger amount) {
        log.info("处理原生币充值请求: chain={}, from={}, amount={}", chainName, fromAddress, amount);
        return depositService.depositNative(chainName, fromAddress, privateKey, amount);
    }

    /**
     * ERC20代币充值
     */
    public String depositERC20(String chainName, String tokenAddress, String fromAddress, 
                              String privateKey, BigInteger amount) {
        log.info("处理ERC20充值请求: chain={}, token={}, from={}, amount={}", 
                chainName, tokenAddress, fromAddress, amount);
        return depositService.depositERC20(chainName, tokenAddress, fromAddress, privateKey, amount);
    }

    /**
     * 批量充值
     */
    public List<String> batchDeposit(String chainName, String fromAddress, String privateKey,
                                    List<DepositService.DepositRequest> deposits) {
        log.info("处理批量充值请求: chain={}, count={}", chainName, deposits.size());
        return depositService.batchDeposit(chainName, fromAddress, privateKey, deposits);
    }

    // ==================== 提现相关 ====================

    /**
     * 申请原生币提现
     */
    public String requestNativeWithdrawal(String chainName, String userAddress, BigInteger amount) {
        log.info("处理原生币提现申请: chain={}, user={}, amount={}", chainName, userAddress, amount);
        return withdrawalService.requestNativeWithdrawal(chainName, userAddress, amount);
    }

    /**
     * 申请ERC20提现
     */
    public String requestERC20Withdrawal(String chainName, String tokenAddress, 
                                        String userAddress, BigInteger amount) {
        log.info("处理ERC20提现申请: chain={}, token={}, user={}, amount={}", 
                chainName, tokenAddress, userAddress, amount);
        return withdrawalService.requestERC20Withdrawal(chainName, tokenAddress, userAddress, amount);
    }

    /**
     * 执行提现（管理员操作）
     */
    public String executeWithdrawal(String chainName, String recipient, String privateKey,
                                   BigInteger amount, boolean isNative, String tokenAddress) {
        log.info("执行提现: chain={}, recipient={}, amount={}", chainName, recipient, amount);
        return withdrawalService.executeWithdrawal(
            chainName, recipient, privateKey, amount, isNative, tokenAddress
        );
    }

    /**
     * 批量执行提现
     */
    public List<String> batchExecuteWithdrawals(String chainName, String privateKey,
                                               List<WithdrawalService.WithdrawalRequest> withdrawals) {
        log.info("批量执行提现: chain={}, count={}", chainName, withdrawals.size());
        return withdrawalService.batchExecuteWithdrawals(chainName, privateKey, withdrawals);
    }

    // ==================== 查询相关 ====================

    /**
     * 查询用户总资产
     */
    public Map<String, BigInteger> getTotalBalance(String userAddress) {
        log.debug("查询用户总资产: user={}", userAddress);
        return assetQueryService.getTotalBalance(userAddress);
    }

    /**
     * 查询单链余额
     */
    public BigInteger getBalanceOnChain(String chainName, String userAddress) {
        log.debug("查询单链余额: chain={}, user={}", chainName, userAddress);
        return assetQueryService.getUserBalanceOnChain(chainName, userAddress);
    }

    /**
     * 查询ERC20代币余额
     */
    public BigInteger getTokenBalance(String chainName, String tokenAddress, String userAddress) {
        log.debug("查询代币余额: chain={}, token={}, user={}", chainName, tokenAddress, userAddress);
        return assetQueryService.getERC20TokenBalance(chainName, tokenAddress, userAddress);
    }

    /**
     * 查询充值历史
     */
    public List<AssetQueryService.DepositRecord> getDepositHistory(String chainName, 
                                                                   String userAddress, int limit) {
        log.debug("查询充值历史: chain={}, user={}, limit={}", chainName, userAddress, limit);
        return assetQueryService.getDepositHistory(chainName, userAddress, limit);
    }

    /**
     * 查询提现历史
     */
    public List<AssetQueryService.WithdrawalRecord> getWithdrawalHistory(String chainName, 
                                                                         String userAddress, int limit) {
        log.debug("查询提现历史: chain={}, user={}, limit={}", chainName, userAddress, limit);
        return assetQueryService.getWithdrawalHistory(chainName, userAddress, limit);
    }

    // ==================== 事件监听相关 ====================

    /**
     * 监听充值事件
     */
    public List<EventListenerService.DepositEvent> listenDeposits(String chainName, 
                                                                  BigInteger fromBlock, BigInteger toBlock) {
        log.debug("监听充值事件: chain={}, from={}, to={}", chainName, fromBlock, toBlock);
        return eventListenerService.listenDepositEvents(chainName, fromBlock, toBlock);
    }

    /**
     * 监听提现事件
     */
    public List<EventListenerService.WithdrawalEvent> listenWithdrawals(String chainName, 
                                                                        BigInteger fromBlock, BigInteger toBlock) {
        log.debug("监听提现事件: chain={}, from={}, to={}", chainName, fromBlock, toBlock);
        return eventListenerService.listenWithdrawalEvents(chainName, fromBlock, toBlock);
    }

    /**
     * 启动持续事件监听
     */
    public void startContinuousListening(String chainName, BigInteger lastBlock, long pollInterval) {
        log.info("启动持续事件监听: chain={}, lastBlock={}, interval={}ms", 
                chainName, lastBlock, pollInterval);
        eventListenerService.startContinuousListening(chainName, lastBlock, pollInterval);
    }
}
