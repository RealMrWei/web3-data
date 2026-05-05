package com.web3.service;

import com.web3.contract.AssetToken;
import com.web3.contract.DepositVault;
import com.web3.contract.WithdrawalManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthGasPrice;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.tx.gas.StaticGasProvider;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Web3 交易服务 - 使用生成的合约类发送交易
 */
@Slf4j
@Service
public class Web3TransactionService {

        @Autowired
        @org.springframework.beans.factory.annotation.Qualifier("defaultWeb3j")
        private Web3j web3j;

        @Value("${web3.private-key}")
        private String privateKey;

        @Value("${web3.contracts.asset-token}")
        private String assetTokenAddress;

        @Value("${web3.contracts.deposit-vault}")
        private String depositVaultAddress;

        @Value("${web3.contracts.withdrawal-manager}")
        private String withdrawalManagerAddress;

        @Value("${web3.gas.gas-limit:500000}")
        private BigInteger gasLimit;

        @Value("${web3.gas.max-fee-per-gas:50000000000}")
        private BigInteger maxFeePerGas;

        @Value("${web3.gas.max-priority-fee-per-gas:1500000000}")
        private BigInteger maxPriorityFeePerGas;

        /**
         * 创建动态 Gas Provider（优先使用 EIP-1559，回退到传统模式）
         */
        private ContractGasProvider createGasProvider() throws Exception {
                try {
                        // 尝试获取网络 Gas Price
                        EthGasPrice gasPriceResponse = web3j.ethGasPrice().send();
                        BigInteger networkGasPrice = gasPriceResponse.getGasPrice();

                        // 增加 20% 余量
                        BigInteger safeGasPrice = networkGasPrice.multiply(BigInteger.valueOf(120))
                                        .divide(BigInteger.valueOf(100));

                        log.info("使用动态 Gas Price: {} wei ({} gwei)", safeGasPrice,
                                        safeGasPrice.divide(BigInteger.valueOf(1000000000)));

                        return new StaticGasProvider(safeGasPrice, gasLimit);
                } catch (Exception e) {
                        log.warn("获取 Gas Price 失败，使用配置值: {}", e.getMessage());
                        return new StaticGasProvider(maxFeePerGas, gasLimit);
                }
        }

        /**
         * 测试充值 - 调用 DepositVault 合约的 deposit 方法
         * 
         * @param tokenAddress 代币合约地址
         * @param amount       充值金额（单位：wei）
         * @return 交易哈希
         */
        public String deposit(String tokenAddress, BigDecimal amount) throws Exception {
                log.info("发送充值交易: token={}, amount={}", tokenAddress, amount);
                log.info("privateKey: {}", privateKey);
                // 1. 获取用户凭证
                Credentials credentials = Credentials.create(privateKey);
                String fromAddress = credentials.getAddress();
                log.info("使用账户: {}", fromAddress);

                // 2. 创建 Gas Provider
                ContractGasProvider gasProvider = createGasProvider();

                // 3. 加载 AssetToken 合约
                AssetToken tokenContract = AssetToken.load(
                                assetTokenAddress,
                                web3j,
                                credentials,
                                gasProvider);

                // 4. 授权 DepositVault 使用代币（approve）
                BigInteger approveAmount = amount.toBigInteger();
                TransactionReceipt approveReceipt = tokenContract.approve(depositVaultAddress, approveAmount).send();
                log.info("✅ Approve 交易成功: txHash={}, gasUsed={}",
                                approveReceipt.getTransactionHash(), approveReceipt.getGasUsed());

                // 等待交易确认
                Thread.sleep(2000);

                // 5. 加载 DepositVault 合约
                DepositVault vaultContract = DepositVault.load(
                                depositVaultAddress,
                                web3j,
                                credentials,
                                gasProvider);

                // 6. 调用 deposit 方法
                TransactionReceipt depositReceipt = vaultContract.deposit(tokenAddress, approveAmount).send();
                log.info("✅ Deposit 交易成功: txHash={}, gasUsed={}",
                                depositReceipt.getTransactionHash(), depositReceipt.getGasUsed());

                return depositReceipt.getTransactionHash();
        }

        /**
         * 测试提现 - 调用 WithdrawalManager 合约的 requestWithdrawal 方法
         * 
         * @param tokenAddress 代币合约地址
         * @param amount       提现金额（单位：wei）
         * @return 交易哈希
         */
        public String requestWithdrawal(String tokenAddress, BigDecimal amount) throws Exception {
                log.info("发送提现请求: token={}, amount={}", tokenAddress, amount);

                // 1. 获取用户凭证
                Credentials credentials = Credentials.create(privateKey);
                String fromAddress = credentials.getAddress();
                log.info("使用账户: {}", fromAddress);

                // 2. 创建 Gas Provider
                ContractGasProvider gasProvider = createGasProvider();

                // 3. 加载 WithdrawalManager 合约
                WithdrawalManager withdrawalContract = WithdrawalManager.load(
                                withdrawalManagerAddress,
                                web3j,
                                credentials,
                                gasProvider);

                // 4. 调用 requestWithdrawal 方法
                TransactionReceipt receipt = withdrawalContract.requestWithdrawal(tokenAddress, amount.toBigInteger())
                                .send();
                log.info("✅ 提现请求交易成功: txHash={}, gasUsed={}",
                                receipt.getTransactionHash(), receipt.getGasUsed());

                return receipt.getTransactionHash();
        }

        /**
         * 【临时跑通版】直接给提现合约转钱（不用DepositVault转出）
         * 不需要修改合约！
         */
        public String transferToWithdrawTemporary(
                        String tokenAddress,
                        BigDecimal amount) throws Exception {
                log.info("【临时】直接给提现合约打款测试", tokenAddress, amount);

                Credentials credentials = Credentials.create(privateKey);
                ContractGasProvider gasProvider = createGasProvider();
                BigInteger transferAmount = amount.toBigInteger();

                // =========================
                // 直接从管理员钱包转给提现合约
                // =========================
                AssetToken token = AssetToken.load(
                                tokenAddress,
                                web3j,
                                credentials,
                                gasProvider);

                TransactionReceipt transferReceipt = token.transfer(
                                withdrawalManagerAddress,
                                transferAmount).send();

                log.info("✅ 转账完成：{}", transferReceipt.getTransactionHash());
                Thread.sleep(2000);

                token.approve(withdrawalManagerAddress, transferAmount).send();
                // =========================
                // 给用户在提现合约记账
                // =========================
                WithdrawalManager wm = WithdrawalManager.load(
                                withdrawalManagerAddress,
                                web3j,
                                credentials,
                                gasProvider);

                TransactionReceipt receipt = wm.depositToVault(
                                tokenAddress,
                                transferAmount).send();

                log.info("✅ 测试完成！用户现在可以提现！");
                return receipt.getTransactionHash();
        }
}