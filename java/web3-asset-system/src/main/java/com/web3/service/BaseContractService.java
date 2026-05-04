package com.web3.service;

import com.web3.chain.MultiChainManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.utils.Numeric;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 多链合约交互基类
 * 提供通用的合约调用、交易签名、Gas估算等功能
 * 合约地址从数据库动态获取
 */
@Slf4j
@Service
public abstract class BaseContractService {

    protected final MultiChainManager multiChainManager;
    protected final ContractAddressService contractAddressService;

    public BaseContractService(MultiChainManager multiChainManager, ContractAddressService contractAddressService) {
        this.multiChainManager = multiChainManager;
        this.contractAddressService = contractAddressService;
    }

    /**
     * 获取指定链和类型的合约地址
     * 
     * @param chainName 链名称
     * @param contractType 合约类型
     * @return 合约地址
     */
    protected String getContractAddress(String chainName, String contractType) {
        return contractAddressService.getContractAddress(chainName, contractType);
    }

    /**
     * 调用合约只读方法（eth_call）
     *
     * @param chainName      链名称
     * @param contractAddress 合约地址
     * @param function       函数对象
     * @return 解码后的返回值
     */
    protected List<Type> callContractFunction(String chainName, String contractAddress, Function function) {
        try {
            Web3j web3j = multiChainManager.getWeb3j(chainName);
            String encodedFunction = FunctionEncoder.encode(function);

            EthCall ethCall = web3j.ethCall(
                Transaction.createEthCallTransaction(null, contractAddress, encodedFunction),
                DefaultBlockParameterName.LATEST
            ).send();

            if (ethCall.hasError()) {
                throw new RuntimeException("合约调用失败: " + ethCall.getError().getMessage());
            }

            return FunctionReturnDecoder.decode(ethCall.getValue(), function.getOutputParameters());
        } catch (IOException e) {
            log.error("调用合约方法失败: chain={}, contract={}", chainName, contractAddress, e);
            throw new RuntimeException("合约调用失败", e);
        }
    }

    /**
     * 发送已签名的交易
     *
     * @param chainName   链名称
     * @param fromAddress 发送方地址
     * @param privateKey  私钥
     * @param toAddress   目标地址
     * @param data        交易数据（编码后的函数调用）
     * @param value       发送的ETH数量（wei）
     * @return 交易哈希
     */
    protected String sendSignedTransaction(String chainName, String fromAddress, String privateKey,
                                           String toAddress, String data, BigInteger value) {
        try {
            Web3j web3j = multiChainManager.getWeb3j(chainName);
            Credentials credentials = Credentials.create(privateKey);

            // 获取nonce
            BigInteger nonce = web3j.ethGetTransactionCount(
                fromAddress,
                DefaultBlockParameterName.PENDING
            ).send().getTransactionCount();

            // 获取Gas价格
            BigInteger gasPrice = web3j.ethGasPrice().send().getGasPrice();

            // Gas限制
            BigInteger gasLimit = BigInteger.valueOf(300000);

            // 构建原始交易（使用兼容的API）
            RawTransaction rawTransaction;
            if (value != null && value.compareTo(BigInteger.ZERO) > 0) {
                // ETH转账
                rawTransaction = RawTransaction.createEtherTransaction(
                    nonce,
                    gasPrice,
                    gasLimit,
                    toAddress,
                    value
                );
            } else {
                // 合约调用
                rawTransaction = RawTransaction.createTransaction(
                    nonce,
                    gasPrice,
                    gasLimit,
                    toAddress,
                    data != null ? data : ""
                );
            }

            // 签名交易
            byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials);
            String hexValue = Numeric.toHexString(signedMessage);

            // 发送交易
            EthSendTransaction ethSendTransaction = web3j.ethSendRawTransaction(hexValue).send();

            if (ethSendTransaction.hasError()) {
                throw new RuntimeException("交易发送失败: " + ethSendTransaction.getError().getMessage());
            }

            String txHash = ethSendTransaction.getTransactionHash();
            log.info("交易发送成功: chain={}, txHash={}", chainName, txHash);
            return txHash;

        } catch (IOException e) {
            log.error("发送交易失败: chain={}, from={}", chainName, fromAddress, e);
            throw new RuntimeException("交易发送失败", e);
        }
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
     * 构建ERC20 transfer函数调用
     *
     * @param recipient 接收地址
     * @param amount    转账金额
     * @return 编码后的函数调用数据
     */
    protected String encodeTransferFunction(String recipient, BigInteger amount) {
        Function function = new Function(
            "transfer",
            Arrays.asList(
                new Address(recipient),
                new Uint256(amount)
            ),
            Collections.emptyList()
        );
        return FunctionEncoder.encode(function);
    }

    /**
     * 查询ERC20代币余额
     *
     * @param chainName    链名称
     * @param tokenAddress 代币合约地址
     * @param userAddress  用户地址
     * @return 余额
     */
    protected BigInteger getERC20Balance(String chainName, String tokenAddress, String userAddress) {
        Function function = new Function(
            "balanceOf",
            Arrays.asList(new Address(userAddress)),
            Collections.singletonList(new TypeReference<Uint256>() {})
        );

        List<Type> result = callContractFunction(chainName, tokenAddress, function);
        return ((Uint256) result.get(0)).getValue();
    }

    /**
     * 查询ERC20代币精度
     *
     * @param chainName    链名称
     * @param tokenAddress 代币合约地址
     * @return 精度（decimals）
     */
    protected int getTokenDecimals(String chainName, String tokenAddress) {
        Function function = new Function(
            "decimals",
            Collections.emptyList(),
            Collections.singletonList(new TypeReference<org.web3j.abi.datatypes.generated.Uint8>() {})
        );

        List<Type> result = callContractFunction(chainName, tokenAddress, function);
        return ((org.web3j.abi.datatypes.generated.Uint8) result.get(0)).getValue().intValue();
    }
}
