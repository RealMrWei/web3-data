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
     * 发起原生币提现请求
     *
     * @param chainName   链名称
     * @param userAddress 用户地址
     * @param amount      提现金额（wei）
     * @return 提现请求ID（需要后端审核后执行）
     */
    public String requestNativeWithdrawal(String chainName, String userAddress, BigInteger amount) {
        log.info("收到原生币提现请求: chain={}, user={}, amount={}", chainName, userAddress, amount);
        
        // TODO: 将提现请求保存到数据库，等待审核
        // 这里返回一个模拟的请求ID
        String requestId = "WD-" + System.currentTimeMillis() + "-" + chainName;
        
        // TODO: 保存提现记录到数据库
        // withdrawalRecordRepository.save(new WithdrawalRecord(...));
        
        return requestId;
    }

    /**
     * 发起ERC20代币提现请求
     *
     * @param chainName      链名称
     * @param tokenAddress   代币合约地址
     * @param userAddress    用户地址
     * @param amount         提现金额
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
     * @param chainName       链名称
     * @param recipient       接收地址
     * @param privateKey      管理员私钥
     * @param amount          提现金额
     * @param isNative        是否为原生币
     * @param tokenAddress    代币地址（仅ERC20需要）
     * @return 交易哈希
     */
    public String executeWithdrawal(String chainName, String recipient, String privateKey,
                                    BigInteger amount, boolean isNative, String tokenAddress) {
        String withdrawalManagerAddress = web3Properties.getContracts().getWithdrawalManager();
        
        log.info("执行提现: chain={}, recipient={}, amount={}, isNative={}", 
                chainName, recipient, amount, isNative);

        if (isNative) {
            // 原生币提现：直接发送ETH
            return sendSignedTransaction(
                chainName, recipient, privateKey, recipient, null, amount
            );
        } else {
            // ERC20提现：调用合约的withdraw方法
            Function withdrawFunction = new Function(
                "withdraw",
                Arrays.asList(
                    new Address(tokenAddress),
                    new Address(recipient),
                    new Uint256(amount)
                ),
                Collections.emptyList()
            );
            
            String data = FunctionEncoder.encode(withdrawFunction);
            return sendSignedTransaction(
                chainName, recipient, privateKey, withdrawalManagerAddress, data, BigInteger.ZERO
            );
        }
    }

    /**
     * 批量执行提现
     *
     * @param chainName    链名称
     * @param privateKey   管理员私钥
     * @param withdrawals  提现列表
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
                withdrawal.getTokenAddress()
            );
            txHashes.add(txHash);
        }
        
        return txHashes;
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
            Collections.singletonList(new TypeReference<Uint256>() {})
        );

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

        public WithdrawalRequest() {}

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
