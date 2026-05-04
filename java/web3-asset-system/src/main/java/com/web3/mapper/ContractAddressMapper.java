package com.web3.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.web3.entity.ContractAddress;
import org.apache.ibatis.annotations.Mapper;

/**
 * 合约地址 Mapper 接口
 */
@Mapper
public interface ContractAddressMapper extends BaseMapper<ContractAddress> {
}
