package com.web3.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.web3.entity.DepositRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 充值记录 Mapper 接口
 */
@Mapper
public interface DepositRecordMapper extends BaseMapper<DepositRecord> {

    /**
     * 批量插入充值记录
     * 
     * @param records 待插入的记录列表
     * @return 成功插入的记录数
     */
    int batchInsert(@Param("list") List<DepositRecord> records);
}
