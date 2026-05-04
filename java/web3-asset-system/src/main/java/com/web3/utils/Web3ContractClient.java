package com.web3.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.*;
import org.web3j.protocol.exceptions.TransactionException;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.gas.DefaultGasProvider;
import org.web3j.utils.Async;
import org.web3j.utils.Numeric;

import java.io.IOException;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 生产级 Web3 合约交互工具类【重构修复版】
 * 修复：阻塞探活、内存泄漏、线程卡死、事件解析类不存在、单私钥绑定、并发nonce冲突
 * 特性：多链、主备RPC切换、超时控制、指数退避重试、Nonce缓存、无阻塞扫块
 */
@Slf4j
@Component
public class Web3ContractClient {

    // 多链Web3j实例
    private final Map<String, Web3j> web3jClients = new ConcurrentHashMap<>();
    // 多链RPC节点列表
    private final Map<String, List<String>> rpcEndpoints = new ConcurrentHashMap<>();
    // 当前使用RPC下标
    private final Map<String, Integer> currentRpcIndex = new ConcurrentHashMap<>();
    // 标记节点是否已故障，避免频繁无效探活
    private final Map<String, AtomicBoolean> chainRpcFaultFlag = new ConcurrentHashMap<>();

    // Nonce 本地缓存：chain:address -> nextNonce
    private final Map<String, BigInteger> nonceCache = new ConcurrentHashMap<>();

    // 重试&超时配置
    private int maxRetries = 3;
    private long baseDelayMs = 1000;
    private long timeoutMs = 30000;
    private final long pollReceiptIntervalMs = 2000;

    // ========================= 链注册 & RPC 容错 =========================
    public void registerChain(String chainName, List<String> endpoints) {
        rpcEndpoints.put(chainName, endpoints);
        currentRpcIndex.put(chainName, 0);
        chainRpcFaultFlag.put(chainName, new AtomicBoolean(false));
        // 初始化第一个RPC
        switchToRpcEndpoint(chainName, 0, null);
    }

    /**
     * 切换RPC，优雅关闭旧Web3j，防止内存泄漏
     */
    private void switchToRpcEndpoint(String chainName, int index, Web3j oldWeb3j) {
        List<String> endpoints = rpcEndpoints.get(chainName);
        if (endpoints == null || endpoints.isEmpty()) {
            throw new IllegalArgumentException("未配置 " + chainName + " 链RPC节点");
        }
        // 关闭旧实例
        if (oldWeb3j != null) {
            try {
                oldWeb3j.shutdown();
            } catch (Exception e) {
                log.warn("关闭旧Web3j实例异常", e);
            }
        }
        int realIdx = index % endpoints.size();
        String endpoint = endpoints.get(realIdx);
        Web3j newWeb3j = Web3j.build(new HttpService(endpoint));
        web3jClients.put(chainName, newWeb3j);
        currentRpcIndex.put(chainName, realIdx);
        chainRpcFaultFlag.get(chainName).set(false);
        log.info("链{} 切换RPC节点: {}", chainName, endpoint);
    }

    /**
     * 获取Web3j：不主动探活，调用异常再切节点，杜绝阻塞
     */
    public Web3j getWeb3j(String chainName) {
        Web3j web3j = web3jClients.get(chainName);
        if (web3j == null) {
            throw new IllegalArgumentException("未初始化链：" + chainName);
        }
        return web3j;
    }

    /**
     * 调用异常时触发RPC故障切换
     */
    private Web3j fallbackRpc(String chainName) {
        AtomicBoolean fault = chainRpcFaultFlag.get(chainName);
        if (!fault.compareAndSet(false, true)) {
            return getWeb3j(chainName);
        }
        int currIdx = currentRpcIndex.getOrDefault(chainName, 0);
        Web3j old = web3jClients.get(chainName);
        switchToRpcEndpoint(chainName, currIdx + 1, old);
        return web3jClients.get(chainName);
    }

    // ========================= 交易发送（不绑定工具类私钥） =========================
    public CompletableFuture<TransactionReceipt> sendTransactionWithRetry(
            String chainName, Credentials credentials,
            String toAddress, String data, BigInteger value) {

        return CompletableFuture.supplyAsync(() -> {
            int attempt = 0;
            Exception lastEx = null;
            Web3j web3j = getWeb3j(chainName);

            while (attempt <= maxRetries) {
                try {
                    // 本地缓存Nonce，解决并发冲突
                    BigInteger nonce = getLocalNonce(chainName, credentials.getAddress());
                    // 估算Gas
                    GasEstimation gasEst = estimateOptimalGas(web3j, toAddress, data, value);
                    // 构造交易
                    RawTransaction rawTx = RawTransaction.createTransaction(
                            nonce, gasEst.gasPrice, gasEst.gasLimit,
                            toAddress, value, data);
                    // 签名发送
                    byte[] signed = TransactionEncoder.signMessage(rawTx, credentials);
                    String hexTx = Numeric.toHexString(signed);

                    EthSendTransaction sendTx = web3j.ethSendRawTransaction(hexTx)
                            .sendAsync()
                            .get(timeoutMs, TimeUnit.MILLISECONDS);

                    if (sendTx.hasError()) {
                        throw new RuntimeException("交易下发失败:" + sendTx.getError().getMessage());
                    }
                    String txHash = sendTx.getTransactionHash();
                    log.info("交易已下发 chain={} hash={} nonce={}", chainName, txHash, nonce);

                    // 自增本地Nonce
                    incrementLocalNonce(chainName, credentials.getAddress());
                    // 异步等待回执
                    return waitForTransactionReceipt(web3j, txHash);

                } catch (Exception e) {
                    lastEx = e;
                    attempt++;
                    // RPC异常触发切换节点
                    if (isRpcError(e)) {
                        web3j = fallbackRpc(chainName);
                    }
                    // 指数退避重试
                    if (attempt <= maxRetries && shouldRetry(e)) {
                        long delay = baseDelayMs * (1L << (attempt - 1));
                        log.warn("交易失败，{}ms后重试 第{}/{} err:{}",
                                delay, attempt, maxRetries, e.getMessage());
                        try {
                            TimeUnit.MILLISECONDS.sleep(delay);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw new RuntimeException("重试中断", ie);
                        }
                    } else {
                        break;
                    }
                }
            }
            log.error("交易重试耗尽仍失败", lastEx);
            throw new RuntimeException("交易发送失败:" + lastEx.getMessage(), lastEx);
        }, Async.defaultExecutorService());
    }

    // ========================= 余额查询 =========================
    public BigInteger getBalance(String chainName, String address) throws Exception {
        Web3j web3j = getWeb3j(chainName);
        return web3j.ethGetBalance(address, DefaultBlockParameterName.LATEST)
                .sendAsync()
                .get(timeoutMs, TimeUnit.MILLISECONDS)
                .getBalance();
    }

    public BigInteger getTokenBalance(String chainName, String tokenAddress, String userAddress) throws Exception {
        Web3j web3j = getWeb3j(chainName);
        String callData = buildBalanceOfCall(userAddress);

        EthCall ethCall = web3j.ethCall(
                org.web3j.protocol.core.methods.request.Transaction
                        .createEthCallTransaction(null, tokenAddress, callData),
                DefaultBlockParameterName.LATEST).sendAsync().get(timeoutMs, TimeUnit.MILLISECONDS);

        if (ethCall.hasError()) {
            throw new RuntimeException("查询代币余额失败:" + ethCall.getError().getMessage());
        }

        // 关键修复：强制泛型类型匹配
        @SuppressWarnings("unchecked")
        List<TypeReference<Type>> outputTypes = (List<TypeReference<Type>>) (List<?>) Collections
                .singletonList(new TypeReference<Uint256>() {
                });
        List<Type> types = FunctionReturnDecoder.decode(ethCall.getValue(), outputTypes);
        if (types.isEmpty()) {
            throw new RuntimeException("解析代币余额为空");
        }
        return (BigInteger) types.get(0).getValue();
    }

    // ========================= 事件扫描 & 官方标准解析 =========================
    public List<EthLog.LogResult> scanEvents(
            String chainName, Event event,
            BigInteger fromBlock, BigInteger toBlock, String contractAddress) throws Exception {

        Web3j web3j = getWeb3j(chainName);
        EthFilter filter = new EthFilter(
                DefaultBlockParameter.valueOf(fromBlock),
                DefaultBlockParameter.valueOf(toBlock),
                contractAddress);
        filter.addSingleTopic(EventEncoder.encode(event));

        EthLog ethLog = web3j.ethGetLogs(filter)
                .sendAsync()
                .get(timeoutMs, TimeUnit.MILLISECONDS);

        if (ethLog.hasError()) {
            throw new RuntimeException("扫描事件失败:" + ethLog.getError().getMessage());
        }
        return ethLog.getLogs();
    }

    /**
     * 官方标准事件解析，替代不存在的EventDecoder
     */
    public List<Type> parseEventLogs(Event event, String data, List<String> topics) {
        try {
            // indexed 参数（从 topics 解析）
            List<Type> indexedValues = new ArrayList<>();
            @SuppressWarnings("unchecked")
            List<TypeReference<?>> indexedParameters = (List<TypeReference<?>>) (List<?>) event.getIndexedParameters();

            // 跳过第一个topic（事件签名）
            for (int i = 0; i < indexedParameters.size(); i++) {
                if (i + 1 >= topics.size())
                    break;
                TypeReference<?> typeRef = indexedParameters.get(i);
                String topicValue = topics.get(i + 1);
                Type decoded = FunctionReturnDecoder.decodeIndexedValue(topicValue, typeRef);
                indexedValues.add(decoded);
            }

            // non-indexed 参数（从 data 解析）
            List<Type> nonIndexedValues = FunctionReturnDecoder.decode(
                    data,
                    event.getNonIndexedParameters());

            // 合并结果
            List<Type> result = new ArrayList<>();
            result.addAll(indexedValues);
            result.addAll(nonIndexedValues);
            return result;

        } catch (Exception e) {
            log.error("事件日志解析失败", e);
            return Collections.emptyList();
        }
    }

    // ========================= 等待交易回执（轻量化不轰炸节点） =========================
    public TransactionReceipt waitForTransactionReceipt(Web3j web3j, String txHash)
            throws Exception {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeoutMs) {
            EthGetTransactionReceipt receiptResp = web3j.ethGetTransactionReceipt(txHash)
                    .sendAsync()
                    .get(timeoutMs / 5, TimeUnit.MILLISECONDS);

            Optional<TransactionReceipt> opt = receiptResp.getTransactionReceipt();
            if (opt.isPresent()) {
                TransactionReceipt receipt = opt.get();
                if (receipt.getBlockNumber() != null) {
                    log.info("交易已确认 hash={} block={}", txHash, receipt.getBlockNumber());
                    return receipt;
                }
            }
            TimeUnit.MILLISECONDS.sleep(pollReceiptIntervalMs);
        }
        throw new TimeoutException("等待交易回执超时:" + txHash);
    }

    // ========================= Gas 估算 =========================
    public GasEstimation estimateOptimalGas(Web3j web3j, String to, String data, BigInteger value) throws Exception {
        BigInteger gasPrice = web3j.ethGasPrice().sendAsync()
                .get(timeoutMs, TimeUnit.MILLISECONDS).getGasPrice();

        org.web3j.protocol.core.methods.request.Transaction tx = org.web3j.protocol.core.methods.request.Transaction
                .createEthCallTransaction(null, to, data);

        BigInteger gasLimit;
        try {
            EthEstimateGas est = web3j.ethEstimateGas(tx)
                    .sendAsync().get(timeoutMs, TimeUnit.MILLISECONDS);
            if (est.hasError()) {
                gasLimit = new DefaultGasProvider().getGasLimit();
            } else {
                gasLimit = est.getAmountUsed().multiply(BigInteger.valueOf(120))
                        .divide(BigInteger.valueOf(100));
            }
        } catch (Exception e) {
            log.warn("Gas估算异常，使用默认值", e);
            gasLimit = new DefaultGasProvider().getGasLimit();
        }
        return new GasEstimation(gasPrice, gasLimit);
    }

    // ========================= Nonce 本地缓存 解决并发冲突 =========================
    private BigInteger getLocalNonce(String chainName, String address) throws Exception {
        String key = chainName + ":" + address;
        if (!nonceCache.containsKey(key)) {
            Web3j web3j = getWeb3j(chainName);
            BigInteger nonce = web3j.ethGetTransactionCount(
                    address, DefaultBlockParameterName.PENDING).sendAsync().get(timeoutMs, TimeUnit.MILLISECONDS)
                    .getTransactionCount();
            nonceCache.put(key, nonce);
        }
        return nonceCache.get(key);
    }

    private void incrementLocalNonce(String chainName, String address) {
        String key = chainName + ":" + address;
        nonceCache.computeIfPresent(key, (k, v) -> v.add(BigInteger.ONE));
    }

    // ========================= 工具私有方法 =========================
    private String buildBalanceOfCall(String userAddress) {
        String method = "70a08231";
        String pad = Numeric.toHexStringWithPrefixZeroPadded(Numeric.toBigInt(userAddress), 64);
        return "0x" + method + pad.substring(2);
    }

    private boolean shouldRetry(Exception e) {
        String msg = e.getMessage().toLowerCase();
        return msg.contains("timeout")
                || msg.contains("connection")
                || msg.contains("network")
                || msg.contains("gas")
                || msg.contains("nonce")
                || msg.contains("insufficient funds");
    }

    private boolean isRpcError(Exception e) {
        return e instanceof IOException
                || e instanceof TimeoutException
                || e.getMessage().toLowerCase().contains("connection");
    }

    // ========================= 内部实体 & Getter/Setter =========================
    public static class GasEstimation {
        public final BigInteger gasPrice;
        public final BigInteger gasLimit;

        public GasEstimation(BigInteger gasPrice, BigInteger gasLimit) {
            this.gasPrice = gasPrice;
            this.gasLimit = gasLimit;
        }
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public long getBaseDelayMs() {
        return baseDelayMs;
    }

    public void setBaseDelayMs(long baseDelayMs) {
        this.baseDelayMs = baseDelayMs;
    }

    public long getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(long timeoutMs) {
        this.timeoutMs = timeoutMs;
    }
}