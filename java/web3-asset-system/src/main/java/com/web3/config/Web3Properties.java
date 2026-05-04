package com.web3.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Web3 多链配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "web3")
public class Web3Properties {

    /**
     * 链配置列表
     */
    private List<ChainConfig> chains;

    /**
     * 合约地址
     */
    private ContractConfig contracts;

    /**
     * 私钥（生产环境应使用 KMS）
     */
    private String privateKey;

    /**
     * Gas 配置
     */
    private GasConfig gas;

    @Data
    public static class ChainConfig {
        /**
         * 链名称：ethereum, bnb, arbitrum
         */
        private String name;

        /**
         * RPC 节点地址
         */
        private String rpcUrl;

        /**
         * 链 ID
         */
        private Long chainId;

        /**
         * 原生币种
         */
        private String nativeCurrency;

        /**
         * 是否启用
         */
        private Boolean enabled = true;
    }

    @Data
    public static class ContractConfig {
        private String assetToken;
        private String depositVault;
        private String withdrawalManager;
    }

    @Data
    public static class GasConfig {
        private Long gasLimit;
        private Long maxFeePerGas;
        private Long maxPriorityFeePerGas;
    }
}
