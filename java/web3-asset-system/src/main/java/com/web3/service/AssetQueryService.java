package com.web3.service;

import com.web3.chain.MultiChainManager;
import com.web3.config.Web3Properties;
import com.web3.entity.ChainConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.generated.Uint256;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 资产查询服务 - 提供多链资产余额、交易记录等查询功能
 */
@Slf4j
@Service
public class AssetQueryService extends BaseContractService {

    private final Web3Properties web3Properties;

    public AssetQueryService(MultiChainManager multiChainManager, 
                             ContractAddressService contractAddressService,
                             Web3Properties web3Properties) {
        super(multiChainManager, contractAddressService);
        this.web3Properties = web3Properties;
    }

    /**
     * 查询用户在所有链上的总资产（以 ETH 计）
     *
     * @param userAddress 用户地址
     * @return 各链的资产映射 {chainName: balance}
     */
    public Map<String, BigInteger> getTotalBalance(String userAddress) {
        Map<String, BigInteger> balances = new HashMap<>();
        
        // 从数据库获取启用的链配置
        List<ChainConfig> chains = multiChainManager.getActiveChains();
        
        for (ChainConfig chain : chains) {
            String chainName = chain.getChainName();
            try {
                BigInteger balance = getUserBalanceOnChain(chainName, userAddress);
                balances.put(chainName, balance);
                log.debug("链 {} 上的余额: {}", chainName, balance);
            } catch (Exception e) {
                log.error("查询链 {} 上的余额失败", chainName, e);
            }
        }
        
        return balances;
    }

    /**
     * 查询用户在指定链上的余额
     *
     * @param chainName   链名称
     * @param userAddress 用户地址
     * @return 余额（wei）
     */
    public BigInteger getUserBalanceOnChain(String chainName, String userAddress) {
        String vaultAddress = web3Properties.getContracts().getDepositVault();
        
        Function function = new Function(
            "balanceOf",
            Arrays.asList(new Address(userAddress)),
            Collections.singletonList(new TypeReference<Uint256>() {})
        );

        return ((Uint256) callContractFunction(chainName, vaultAddress, function).get(0)).getValue();
    }

    /**
     * 查询用户的 ERC20 代币余额
     *
     * @param chainName    链名称
     * @param tokenAddress 代币合约地址
     * @param userAddress  用户地址
     * @return 余额
     */
    public BigInteger getERC20TokenBalance(String chainName, String tokenAddress, String userAddress) {
        return getERC20Balance(chainName, tokenAddress, userAddress);
    }

    /**
     * 查询多个代币的余额
     *
     * @param chainName   链名称
     * @param userAddress 用户地址
     * @param tokens      代币地址列表
     * @return {tokenAddress: balance}
     */
    public Map<String, BigInteger> getMultipleTokenBalances(String chainName, String userAddress, 
                                                            List<String> tokens) {
        Map<String, BigInteger> balances = new HashMap<>();
        
        for (String tokenAddress : tokens) {
            try {
                BigInteger balance = getERC20Balance(chainName, tokenAddress, userAddress);
                balances.put(tokenAddress, balance);
            } catch (Exception e) {
                log.error("查询代币余额失败: token={}", tokenAddress, e);
            }
        }
        
        return balances;
    }

    /**
     * 查询金库合约的总锁仓量
     *
     * @param chainName 链名称
     * @return 总锁仓量
     */
    public BigInteger getTotalDeposited(String chainName) {
        String vaultAddress = web3Properties.getContracts().getDepositVault();
        
        Function function = new Function(
            "totalDeposited",
            Collections.emptyList(),
            Collections.singletonList(new TypeReference<Uint256>() {})
        );

        return ((Uint256) callContractFunction(chainName, vaultAddress, function).get(0)).getValue();
    }

    /**
     * 查询用户的充值历史记录（从数据库或事件日志）
     *
     * @param chainName   链名称
     * @param userAddress 用户地址
     * @param limit       返回数量限制
     * @return 充值记录列表
     */
    public java.util.List<DepositRecord> getDepositHistory(String chainName, String userAddress, int limit) {
        // TODO: 从数据库查询充值记录
        // 或者通过监听事件日志获取
        
        log.info("查询充值历史: chain={}, user={}, limit={}", chainName, userAddress, limit);
        return Collections.emptyList(); // 临时返回空列表
    }

    /**
     * 查询用户的提现历史记录
     *
     * @param chainName   链名称
     * @param userAddress 用户地址
     * @param limit       返回数量限制
     * @return 提现记录列表
     */
    public java.util.List<WithdrawalRecord> getWithdrawalHistory(String chainName, String userAddress, int limit) {
        // TODO: 从数据库查询提现记录
        
        log.info("查询提现历史: chain={}, user={}, limit={}", chainName, userAddress, limit);
        return Collections.emptyList(); // 临时返回空列表
    }

    /**
     * 获取链上最新区块号
     *
     * @param chainName 链名称
     * @return 区块号
     */
    public BigInteger getLatestBlockNumber(String chainName) {
        return multiChainManager.getCurrentBlock(chainName);
    }

    /**
     * 充值记录对象
     */
    public static class DepositRecord {
        private String txHash;
        private String tokenAddress;
        private BigInteger amount;
        private Long timestamp;
        private String status;

        public DepositRecord() {}

        public DepositRecord(String txHash, String tokenAddress, BigInteger amount, 
                           Long timestamp, String status) {
            this.txHash = txHash;
            this.tokenAddress = tokenAddress;
            this.amount = amount;
            this.timestamp = timestamp;
            this.status = status;
        }

        // Getters and Setters
        public String getTxHash() { return txHash; }
        public void setTxHash(String txHash) { this.txHash = txHash; }
        
        public String getTokenAddress() { return tokenAddress; }
        public void setTokenAddress(String tokenAddress) { this.tokenAddress = tokenAddress; }
        
        public BigInteger getAmount() { return amount; }
        public void setAmount(BigInteger amount) { this.amount = amount; }
        
        public Long getTimestamp() { return timestamp; }
        public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    /**
     * 提现记录对象
     */
    public static class WithdrawalRecord {
        private String txHash;
        private String tokenAddress;
        private BigInteger amount;
        private Long timestamp;
        private String status;

        public WithdrawalRecord() {}

        public WithdrawalRecord(String txHash, String tokenAddress, BigInteger amount,
                               Long timestamp, String status) {
            this.txHash = txHash;
            this.tokenAddress = tokenAddress;
            this.amount = amount;
            this.timestamp = timestamp;
            this.status = status;
        }

        // Getters and Setters
        public String getTxHash() { return txHash; }
        public void setTxHash(String txHash) { this.txHash = txHash; }
        
        public String getTokenAddress() { return tokenAddress; }
        public void setTokenAddress(String tokenAddress) { this.tokenAddress = tokenAddress; }
        
        public BigInteger getAmount() { return amount; }
        public void setAmount(BigInteger amount) { this.amount = amount; }
        
        public Long getTimestamp() { return timestamp; }
        public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
}
