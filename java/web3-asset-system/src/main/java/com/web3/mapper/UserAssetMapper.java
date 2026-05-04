package com.web3.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.web3.entity.UserAsset;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户资产 Mapper 接口
 */
@Mapper
public interface UserAssetMapper extends BaseMapper<UserAsset> {
}
