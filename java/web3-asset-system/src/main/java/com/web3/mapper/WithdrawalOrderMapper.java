package com.web3.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.web3.entity.WithdrawalOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 提现订单 Mapper 接口
 */
@Mapper
public interface WithdrawalOrderMapper extends BaseMapper<WithdrawalOrder> {

    /**
     * 批量插入提现订单
     * 
     * @param orders 待插入的订单列表
     * @return 成功插入的订单数
     */
    int batchInsert(@Param("list") List<WithdrawalOrder> orders);
}
