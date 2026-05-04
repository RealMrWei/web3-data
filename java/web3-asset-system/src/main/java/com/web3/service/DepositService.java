package com.web3.service;

import com.web3.chain.MultiChainManager;
import com.web3.config.Web3Properties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.generated.Uint256;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 充值服务 - 支持多链原生币和ERC20代币充值
 */
@Slf4j
@Service
public class DepositService extends BaseContractService {

    private final Web3Properties web3Properties;

    public DepositService(MultiChainManager multiChainManager, 
                          ContractAddressService contractAddressService,
                          Web3Properties web3Properties) {
        super(multiChainManager, contractAddressService);
        this.web3Properties = web3Properties;
    }

    /**
     * 查询用户在金库合约中的余额
     *
     * @param chainName   链名称
     * @param userAddress 用户地址
     * @return 余额（wei）
     */
    public BigInteger getUserBalance(String chainName, String userAddress) {
        String vaultAddress = web3Properties.getContracts().getDepositVault();
        
        Function function = new Function(
            "balanceOf",
            Arrays.asList(new Address(userAddress)),
            Collections.singletonList(new TypeReference<Uint256>() {})
        );

        return ((Uint256) callContractFunction(chainName, vaultAddress, function).get(0)).getValue();
    }

    /**
     * 原生币充值（直接发送ETH到金库合约）
     *
     * @param chainName   链名称
     * @param fromAddress 发送方地址
     * @param privateKey  私钥
     * @param amount      充值金额（wei）
     * @return 交易哈希
     */
    public String depositNative(String chainName, String fromAddress, String privateKey, BigInteger amount) {
        String vaultAddress = web3Properties.getContracts().getDepositVault();
        
        log.info("开始原生币充值: chain={}, from={}, amount={}", chainName, fromAddress, amount);
        
        // 原生币充值直接发送ETH，不需要编码函数调用
        return sendSignedTransaction(chainName, fromAddress, privateKey, vaultAddress, null, amount);
    }

    /**
     * 构建ERC20 approve函数调用
     *
     * @param spender 授权地址
     * @param amount  授权金额
     * @return 编码后的函数调用数据
     */
    protected String encodeApproveFunction(String spender, BigInteger amount) {
        Function function = new Function(
            "approve",
            Arrays.asList(
                new Address(spender),
                new Uint256(amount)
            ),
            Collections.emptyList()
        );
        return FunctionEncoder.encode(function);
    }

    /**
     * ERC20代币充值（两步：先approve，再调用合约deposit）
     *
     * @param chainName      链名称
     * @param tokenAddress   代币合约地址
     * @param fromAddress    发送方地址
     * @param privateKey     私钥
     * @param amount         充值金额
     * @return 交易哈希（deposit交易）
     */
    public String depositERC20(String chainName, String tokenAddress, String fromAddress, 
                               String privateKey, BigInteger amount) {
        String vaultAddress = web3Properties.getContracts().getDepositVault();
        
        log.info("开始ERC20充值: chain={}, token={}, from={}, amount={}", 
                chainName, tokenAddress, fromAddress, amount);

        // 第一步：授权金库合约使用代币
        String approveData = encodeApproveFunction(vaultAddress, amount);
        String approveTxHash = sendSignedTransaction(
            chainName, fromAddress, privateKey, tokenAddress, approveData, BigInteger.ZERO
        );
        log.info("ERC20授权交易已发送: txHash={}", approveTxHash);

        // TODO: 等待授权交易确认后，再执行第二步
        
        // 第二步：调用金库合约的depositERC20方法
        // 注意：这里需要根据实际合约的函数签名调整
        Function depositFunction = new Function(
            "depositERC20",
            Arrays.asList(
                new Address(tokenAddress),
                new Uint256(amount)
            ),
            Collections.emptyList()
        );
        String depositData = FunctionEncoder.encode(depositFunction);
        String depositTxHash = sendSignedTransaction(
            chainName, fromAddress, privateKey, vaultAddress, depositData, BigInteger.ZERO
        );
        
        log.info("ERC20充值交易已发送: txHash={}", depositTxHash);
        return depositTxHash;
    }

    /**
     * 批量充值（支持多个代币）
     *
     * @param chainName   链名称
     * @param fromAddress 发送方地址
     * @param privateKey  私钥
     * @param deposits    充值列表 [{tokenAddress, amount}]
     * @return 交易哈希列表
     */
    public List<String> batchDeposit(String chainName, String fromAddress, String privateKey,
                                               List<DepositRequest> deposits) {
        List<String> txHashes = new ArrayList<>();
        
        for (DepositRequest deposit : deposits) {
            String txHash;
            if (deposit.getTokenAddress() == null || deposit.getTokenAddress().isEmpty()) {
                // 原生币充值
                txHash = depositNative(chainName, fromAddress, privateKey, deposit.getAmount());
            } else {
                // ERC20充值
                txHash = depositERC20(chainName, deposit.getTokenAddress(), fromAddress, 
                                     privateKey, deposit.getAmount());
            }
            txHashes.add(txHash);
        }
        
        return txHashes;
    }

    /**
     * 充值请求对象
     */
    public static class DepositRequest {
        private String tokenAddress; // 为空表示原生币
        private BigInteger amount;

        public DepositRequest() {}

        public DepositRequest(String tokenAddress, BigInteger amount) {
            this.tokenAddress = tokenAddress;
            this.amount = amount;
        }

        public String getTokenAddress() {
            return tokenAddress;
        }

        public void setTokenAddress(String tokenAddress) {
            this.tokenAddress = tokenAddress;
        }

        public BigInteger getAmount() {
            return amount;
        }

        public void setAmount(BigInteger amount) {
            this.amount = amount;
        }
    }
}
