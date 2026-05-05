package com.web3.service;

import com.web3.chain.MultiChainManager;
import com.web3.config.Web3Properties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 提现服务 - 支持多链原生币和ERC20代币提现
 */
@Slf4j
@Service
public class WithdrawalService extends BaseContractService {

    private final Web3Properties web3Properties;

    public WithdrawalService(MultiChainManager multiChainManager,
            ContractAddressService contractAddressService,
            Web3Properties web3Properties) {
        super(multiChainManager, contractAddressService);
        this.web3Properties = web3Properties;
    }

    /**
     * 申请提现（用户调用）
     */
    public String requestWithdrawal(String chainName,
            String userPrivateKey,
            String tokenAddress,
            BigInteger amount) {
        String contract = web3Properties.getContracts().getWithdrawalManager();

        Function func = new Function(
                "requestWithdrawal",
                Arrays.asList(
                        new Address(tokenAddress),
                        new Uint256(amount)),
                Collections.emptyList());

        String data = FunctionEncoder.encode(func);
        Credentials credentials = Credentials.create(userPrivateKey);

        return sendSignedTransaction(
                chainName,
                credentials.getAddress(),
                userPrivateKey,
                contract,
                data,
                BigInteger.ZERO);
    }

    // 查询：下一个提现ID（也就是最新申请的ID）
    public BigInteger getNextWithdrawalId(String chainName) {
        try {
            String contract = web3Properties.getContracts().getWithdrawalManager();

            Function function = new Function(
                    "withdrawalCount",
                    Collections.emptyList(),
                    Collections.singletonList(new TypeReference<Uint256>() {
                    }));

            List<Type> result = callContractFunction(chainName, contract, function);
            if (result == null || result.isEmpty()) {
                return BigInteger.ZERO;
            }

            BigInteger count = ((Uint256) result.get(0)).getValue();
            // 当前 count 就是下一个ID，最新申请的ID = count - 1
            return count.subtract(BigInteger.ONE);
        } catch (Exception e) {
            log.error("查询最新提现ID失败", e);
            return BigInteger.ZERO;
        }
    }

    // 查询提现记录：状态、用户、金额、token
    public Map<String, Object> getWithdrawalRecord(String chainName, BigInteger withdrawalId) {
        try {
            String contract = web3Properties.getContracts().getWithdrawalManager();

            Function function = new Function(
                    "getWithdrawalRecord",
                    Arrays.asList(new Uint256(withdrawalId)),
                    Arrays.asList(
                            new TypeReference<Address>() {
                            }, // user
                            new TypeReference<Address>() {
                            }, // token
                            new TypeReference<Uint256>() {
                            }, // amount
                            new TypeReference<Uint256>() {
                            }, // timestamp
                            new TypeReference<Uint256>() {
                            }, // status
                            new TypeReference<Address>() {
                            }, // approver
                            new TypeReference<Uint256>() {
                            } // completedAt
                    ));

            List<Type> result = callContractFunction(chainName, contract, function);
            if (result == null || result.size() < 7) {
                return null;
            }

            Map<String, Object> map = new HashMap<>();
            map.put("user", ((Address) result.get(0)).getValue());
            map.put("token", ((Address) result.get(1)).getValue());
            map.put("amount", ((Uint256) result.get(2)).getValue());
            map.put("status", ((Uint256) result.get(4)).getValue()); // 0=待审批 1=已通过 2=拒绝 3=完成
            return map;
        } catch (Exception e) {
            log.error("查询提现记录失败", e);
            return null;
        }
    }

    /**
     * 审批提现
     */
    public String approveWithdrawal(String chainName,
            String adminKey,
            BigInteger withdrawalId) {
        String contract = web3Properties.getContracts().getWithdrawalManager();

        Function func = new Function(
                "approveWithdrawal",
                Arrays.asList(new Uint256(withdrawalId)),
                Collections.emptyList());

        String data = FunctionEncoder.encode(func);
        Credentials credentials = Credentials.create(adminKey);

        return sendSignedTransaction(
                chainName,
                credentials.getAddress(),
                adminKey,
                contract,
                data,
                BigInteger.ZERO);
    }

    /**
     * 发起ERC20代币提现请求
     *
     * @param chainName    链名称
     * @param tokenAddress 代币合约地址
     * @param userAddress  用户地址
     * @param amount       提现金额
     * @return 提现请求ID
     */
    public String requestERC20Withdrawal(String chainName, String tokenAddress,
            String userAddress, BigInteger amount) {
        log.info("收到ERC20提现请求: chain={}, token={}, user={}, amount={}",
                chainName, tokenAddress, userAddress, amount);

        // TODO: 将提现请求保存到数据库，等待审核
        String requestId = "WD-" + System.currentTimeMillis() + "-" + chainName;

        return requestId;
    }

    /**
     * 执行提现（由管理员或自动化系统调用）
     *
     * @param chainName    链名称
     * @param recipient    接收地址
     * @param privateKey   管理员私钥
     * @param amount       提现金额
     * @param isNative     是否为原生币
     * @param tokenAddress 代币地址（仅ERC20需要）
     * @return 交易哈希
     */
    public String executeWithdrawal(String chainName, String recipient, String privateKey,
            BigInteger amount, boolean isNative, String tokenAddress) {
        String withdrawalManagerAddress = web3Properties.getContracts().getWithdrawalManager();

        log.info("执行提现: chain={}, recipient={}, amount={}, isNative={}",
                chainName, recipient, amount, isNative);
        String withdrawContract = web3Properties.getContracts().getWithdrawalManager();
        log.info("提现合约地址: {}", withdrawContract);
        if (isNative) {
            // 原生币提现：直接发送ETH
            return sendSignedTransaction(
                    chainName, recipient, privateKey, recipient, null, amount);
        } else {
            // =======================
            // ERC20 代理合约提现
            // =======================

            // 1. 授权检查：用户必须授权给【代理合约地址】
            BigInteger allowance = getAllowance(chainName, tokenAddress, recipient, withdrawContract);

            log.info("授权检查：用户授权额度={} | 提现金额={}", allowance, amount);
            if (allowance.compareTo(amount) < 0) {
                throw new RuntimeException("授权额度不足，请先授权给代理合约");
            }

            // 2. 调用代理合约 withdraw 方法
            Function withdrawFunc = new Function(
                    "executeWithdrawal",
                    Arrays.asList(
                            new Address(tokenAddress),
                            new Address(recipient),
                            new Uint256(amount)),
                    Collections.emptyList());

            String data = FunctionEncoder.encode(withdrawFunc);

            // 3. 管理员发送交易到代理合约
            return sendSignedTransaction(
                    chainName,
                    recipient,
                    privateKey,
                    withdrawContract,
                    data,
                    BigInteger.ZERO);
        }
    }

    public String executeWithdrawal(
            String chainName,
            String adminPrivateKey,
            BigInteger withdrawalId // 只需要提现ID！
    ) {
        String withdrawContract = web3Properties.getContracts().getWithdrawalManager();

        // ✅ 合约真实方法：executeWithdrawal(uint256 withdrawalId)
        Function function = new Function(
                "executeWithdrawal",
                Arrays.asList(new Uint256(withdrawalId)), // ✅ 只传ID
                Collections.emptyList());

        String data = FunctionEncoder.encode(function);
        Credentials credentials = Credentials.create(adminPrivateKey);

        return sendSignedTransaction(
                chainName,
                credentials.getAddress(),
                adminPrivateKey,
                withdrawContract,
                data,
                BigInteger.ZERO);
    }

    /**
     * 批量执行提现
     *
     * @param chainName   链名称
     * @param privateKey  管理员私钥
     * @param withdrawals 提现列表
     * @return 交易哈希列表
     */
    public List<String> batchExecuteWithdrawals(String chainName, String privateKey,
            List<WithdrawalRequest> withdrawals) {
        List<String> txHashes = new ArrayList<>();

        for (WithdrawalRequest withdrawal : withdrawals) {
            String txHash = executeWithdrawal(
                    chainName,
                    withdrawal.getRecipient(),
                    privateKey,
                    withdrawal.getAmount(),
                    withdrawal.isNative(),
                    withdrawal.getTokenAddress());
            txHashes.add(txHash);
        }

        return txHashes;
    }

    // ==========================
    // 【正确】查询 ERC20 授权额度
    // ==========================
    public BigInteger getAllowance(String chainName, String tokenAddress, String owner, String spender) {
        log.info("查询授权额度: chain={}, token={}, owner={}, spender={}",
                chainName, tokenAddress, owner, spender);
        Function func = new Function(
                "allowance",
                Arrays.asList(new Address(owner), new Address(spender)),
                Collections.singletonList(new TypeReference<Uint256>() {
                }));

        List<Type> result = callContractFunction(chainName, tokenAddress, func);
        if (result == null || result.isEmpty()) {
            log.warn("合约返回空结果，返回授权额度 0");
            return BigInteger.ZERO;
        }
        return ((Uint256) result.get(0)).getValue();
    }

    // ==========================
    // 【测试专用】模拟用户授权
    // ==========================
    public String testApprove(String chainName, String tokenAddress, String userPrivateKey, BigInteger amount) {
        String withdrawContract = web3Properties.getContracts().getWithdrawalManager();
        log.info("模拟用户授权: chain={}, token={}, user={}, amount={}",
                chainName, tokenAddress, userPrivateKey, amount);
        Function func = new Function(
                "approve",
                Arrays.asList(new Address(withdrawContract), new Uint256(amount)),
                Collections.emptyList());

        String data = FunctionEncoder.encode(func);
        Credentials credentials = Credentials.create(userPrivateKey);

        return sendSignedTransaction(
                chainName,
                credentials.getAddress(),
                userPrivateKey,
                tokenAddress,
                data,
                BigInteger.ZERO);
    }

    /**
     * 查询可提现余额
     *
     * @param chainName   链名称
     * @param userAddress 用户地址
     * @return 可提现余额
     */
    public BigInteger getWithdrawableBalance(String chainName, String userAddress) {
        String withdrawalManagerAddress = web3Properties.getContracts().getWithdrawalManager();

        Function function = new Function(
                "getWithdrawableBalance",
                Arrays.asList(new Address(userAddress)),
                Collections.singletonList(new TypeReference<Uint256>() {
                }));

        return ((Uint256) callContractFunction(chainName, withdrawalManagerAddress, function).get(0)).getValue();
    }

    /**
     * 提现请求对象
     */
    public static class WithdrawalRequest {
        private String recipient;
        private BigInteger amount;
        private boolean isNative;
        private String tokenAddress;

        public WithdrawalRequest() {
        }

        public WithdrawalRequest(String recipient, BigInteger amount, boolean isNative, String tokenAddress) {
            this.recipient = recipient;
            this.amount = amount;
            this.isNative = isNative;
            this.tokenAddress = tokenAddress;
        }

        public String getRecipient() {
            return recipient;
        }

        public void setRecipient(String recipient) {
            this.recipient = recipient;
        }

        public BigInteger getAmount() {
            return amount;
        }

        public void setAmount(BigInteger amount) {
            this.amount = amount;
        }

        public boolean isNative() {
            return isNative;
        }

        public void setNative(boolean nativeFlag) {
            isNative = nativeFlag;
        }

        public String getTokenAddress() {
            return tokenAddress;
        }

        public void setTokenAddress(String tokenAddress) {
            this.tokenAddress = tokenAddress;
        }
    }
}
