package com.web3.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.web3.entity.DepositRecord;
import com.web3.mapper.DepositRecordMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 充值相关 REST API
 * 
 * 功能：
 * 1. ✅ 查询充值记录列表（分页）
 * 2. ✅ 根据 txHash 查询单笔充值
 * 3. ✅ 根据用户地址查询充值历史
 * 4. ✅ 统计充值数据
 */
@Slf4j
@RestController
@RequestMapping("/api/deposit")
public class DepositController {

    @Autowired
    private DepositRecordMapper depositRecordMapper;

    /**
     * 查询充值记录列表（分页）
     * 
     * @param page      页码（从1开始）
     * @param size      每页大小
     * @param chainName 链名称（可选）
     * @param status    状态（可选）
     * @return 分页结果
     */
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> getDepositList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String chainName,
            @RequestParam(required = false) Integer status) {

        log.info("查询充值记录列表: page={}, size={}, chainName={}, status={}", page, size, chainName, status);

        // 构建查询条件
        LambdaQueryWrapper<DepositRecord> wrapper = new LambdaQueryWrapper<>();

        if (chainName != null && !chainName.isEmpty()) {
            wrapper.eq(DepositRecord::getChainName, chainName);
        }

        if (status != null) {
            wrapper.eq(DepositRecord::getStatus, status);
        }

        // 按创建时间倒序
        wrapper.orderByDesc(DepositRecord::getCreateTime);

        // 分页查询
        Page<DepositRecord> pageParam = new Page<>(page, size);
        Page<DepositRecord> result = depositRecordMapper.selectPage(pageParam, wrapper);

        // 构建返回结果
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "success");
        response.put("data", result.getRecords());
        response.put("total", result.getTotal());
        response.put("current", result.getCurrent());
        response.put("size", result.getSize());
        response.put("pages", result.getPages());

        return ResponseEntity.ok(response);
    }

    /**
     * 根据 txHash 查询单笔充值记录
     * 
     * @param txHash 交易哈希
     * @return 充值记录
     */
    @GetMapping("/tx/{txHash}")
    public ResponseEntity<Map<String, Object>> getDepositByTxHash(@PathVariable String txHash) {
        log.info("查询充值记录: txHash={}", txHash);

        LambdaQueryWrapper<DepositRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DepositRecord::getTxHash, txHash);

        DepositRecord record = depositRecordMapper.selectOne(wrapper);

        Map<String, Object> response = new HashMap<>();
        if (record != null) {
            response.put("code", 200);
            response.put("message", "success");
            response.put("data", record);
        } else {
            response.put("code", 404);
            response.put("message", "充值记录不存在");
        }

        return ResponseEntity.ok(response);
    }

    /**
     * 根据用户地址查询充值历史
     * 
     * @param userAddress 用户钱包地址
     * @param page        页码
     * @param size        每页大小
     * @return 充值历史列表
     */
    @GetMapping("/user/{userAddress}")
    public ResponseEntity<Map<String, Object>> getUserDeposits(
            @PathVariable String userAddress,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {

        log.info("查询用户充值历史: userAddress={}, page={}, size={}", userAddress, page, size);

        LambdaQueryWrapper<DepositRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DepositRecord::getUserAddress, userAddress);
        wrapper.orderByDesc(DepositRecord::getCreateTime);

        Page<DepositRecord> pageParam = new Page<>(page, size);
        Page<DepositRecord> result = depositRecordMapper.selectPage(pageParam, wrapper);

        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "success");
        response.put("data", result.getRecords());
        response.put("total", result.getTotal());
        response.put("current", result.getCurrent());
        response.put("size", result.getSize());
        response.put("pages", result.getPages());

        return ResponseEntity.ok(response);
    }

    /**
     * 统计充值数据
     * 
     * @param chainName 链名称（可选）
     * @return 统计数据
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getDepositStatistics(
            @RequestParam(required = false) String chainName) {

        log.info("统计充值数据: chainName={}", chainName);

        LambdaQueryWrapper<DepositRecord> wrapper = new LambdaQueryWrapper<>();

        if (chainName != null && !chainName.isEmpty()) {
            wrapper.eq(DepositRecord::getChainName, chainName);
        }

        // 总充值笔数
        Long totalCount = depositRecordMapper.selectCount(wrapper);

        // 成功充值笔数
        LambdaQueryWrapper<DepositRecord> successWrapper = new LambdaQueryWrapper<>();
        successWrapper.eq(DepositRecord::getStatus, 1);
        if (chainName != null && !chainName.isEmpty()) {
            successWrapper.eq(DepositRecord::getChainName, chainName);
        }
        Long successCount = depositRecordMapper.selectCount(successWrapper);

        // 待确认充值笔数
        LambdaQueryWrapper<DepositRecord> pendingWrapper = new LambdaQueryWrapper<>();
        pendingWrapper.eq(DepositRecord::getStatus, 0);
        if (chainName != null && !chainName.isEmpty()) {
            pendingWrapper.eq(DepositRecord::getChainName, chainName);
        }
        Long pendingCount = depositRecordMapper.selectCount(pendingWrapper);

        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "success");

        Map<String, Object> statistics = new HashMap<>();
        statistics.put("totalCount", totalCount);
        statistics.put("successCount", successCount);
        statistics.put("pendingCount", pendingCount);
        statistics.put("failedCount", totalCount - successCount - pendingCount);

        response.put("data", statistics);

        return ResponseEntity.ok(response);
    }

}
