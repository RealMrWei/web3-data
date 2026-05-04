package com.web3.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.web3.entity.ContractAddress;
import com.web3.mapper.ContractAddressMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * 合约地址配置服务 - 从数据库管理多链合约地址
 */
@Slf4j
@Service
public class ContractAddressService {

    @Autowired
    private ContractAddressMapper contractAddressMapper;

    /**
     * 初始化日志
     */
    @PostConstruct
    public void init() {
        log.info("合约地址配置服务初始化完成");
    }

    /**
     * 根据链名称和合约类型获取合约地址（带缓存）
     * 
     * @param chainName 链名称
     * @param contractType 合约类型
     * @return 合约地址
     */
    @Cacheable(value = "contractAddress", key = "#chainName + ':' + #contractType")
    public String getContractAddress(String chainName, String contractType) {
        LambdaQueryWrapper<ContractAddress> query = new LambdaQueryWrapper<>();
        query.eq(ContractAddress::getChainName, chainName)
             .eq(ContractAddress::getContractType, contractType)
             .eq(ContractAddress::getStatus, 1);
        
        ContractAddress config = contractAddressMapper.selectOne(query);
        if (config == null) {
            throw new IllegalArgumentException("未找到合约地址: chain=" + chainName + ", type=" + contractType);
        }
        
        log.debug("获取合约地址 - 链: {}, 类型: {}, 地址: {}", chainName, contractType, config.getContractAddress());
        return config.getContractAddress();
    }

    /**
     * 添加新的合约地址配置
     */
    @CacheEvict(value = "contractAddress", key = "#contractAddress.chainName + ':' + #contractAddress.contractType")
    public boolean addContractAddress(ContractAddress contractAddress) {
        int result = contractAddressMapper.insert(contractAddress);
        log.info("添加合约地址配置 - 链: {}, 类型: {}, 地址: {}", 
                contractAddress.getChainName(), 
                contractAddress.getContractType(), 
                contractAddress.getContractAddress());
        return result > 0;
    }

    /**
     * 更新合约地址配置
     */
    @CacheEvict(value = "contractAddress", key = "#contractAddress.chainName + ':' + #contractAddress.contractType")
    public boolean updateContractAddress(ContractAddress contractAddress) {
        int result = contractAddressMapper.updateById(contractAddress);
        log.info("更新合约地址配置 - 链: {}, 类型: {}, 地址: {}", 
                contractAddress.getChainName(), 
                contractAddress.getContractType(), 
                contractAddress.getContractAddress());
        return result > 0;
    }

    /**
     * 查询指定链的所有合约地址
     */
    @Cacheable(value = "contractAddress", key = "'chain:' + #chainName")
    public List<ContractAddress> getByChainName(String chainName) {
        LambdaQueryWrapper<ContractAddress> query = new LambdaQueryWrapper<>();
        query.eq(ContractAddress::getChainName, chainName)
             .eq(ContractAddress::getStatus, 1)
             .orderByAsc(ContractAddress::getContractType);
        
        return contractAddressMapper.selectList(query);
    }

    /**
     * 查询所有启用的合约地址
     */
    @Cacheable(value = "contractAddress", key = "'allActive'")
    public List<ContractAddress> getAllActive() {
        LambdaQueryWrapper<ContractAddress> query = new LambdaQueryWrapper<>();
        query.eq(ContractAddress::getStatus, 1)
             .orderByAsc(ContractAddress::getChainName)
             .orderByAsc(ContractAddress::getContractType);
        
        return contractAddressMapper.selectList(query);
    }

    /**
     * 禁用合约地址
     */
    @CacheEvict(value = "contractAddress", allEntries = true)
    public boolean disableContract(String chainName, String contractType) {
        LambdaQueryWrapper<ContractAddress> query = new LambdaQueryWrapper<>();
        query.eq(ContractAddress::getChainName, chainName)
             .eq(ContractAddress::getContractType, contractType);
        
        ContractAddress config = contractAddressMapper.selectOne(query);
        if (config != null) {
            config.setStatus(0);
            return contractAddressMapper.updateById(config) > 0;
        }
        return false;
    }
}
