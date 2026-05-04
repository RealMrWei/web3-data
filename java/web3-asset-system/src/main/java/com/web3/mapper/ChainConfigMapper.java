package com.web3.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.web3.entity.ChainConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * 链配置 Mapper 接口
 */
@Mapper
public interface ChainConfigMapper extends BaseMapper<ChainConfig> {
}
