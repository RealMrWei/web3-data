package com.web3.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.web3.entity.WithdrawalOrder;
import com.web3.mapper.WithdrawalOrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 提现相关 REST API
 * 
 * 功能：
 * 1. ✅ 查询提现订单列表（分页）
 * 2. ✅ 根据订单号查询单笔提现
 * 3. ✅ 根据用户地址查询提现历史
 * 4. ✅ 统计提现数据
 */
@Slf4j
@RestController
@RequestMapping("/api/withdrawal")
public class WithdrawalController {
    
    @Autowired
    private WithdrawalOrderMapper withdrawalOrderMapper;
    
    /**
     * 查询提现订单列表（分页）
     * 
     * @param page 页码（从1开始）
     * @param size 每页大小
     * @param chainName 链名称（可选）
     * @param status 状态（可选）
     * @return 分页结果
     */
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> getWithdrawalList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String chainName,
            @RequestParam(required = false) Integer status) {
        
        log.info("查询提现订单列表: page={}, size={}, chainName={}, status={}", page, size, chainName, status);
        
        // 构建查询条件
        LambdaQueryWrapper<WithdrawalOrder> wrapper = new LambdaQueryWrapper<>();
        
        if (chainName != null && !chainName.isEmpty()) {
            wrapper.eq(WithdrawalOrder::getChainName, chainName);
        }
        
        if (status != null) {
            wrapper.eq(WithdrawalOrder::getStatus, status);
        }
        
        // 按创建时间倒序
        wrapper.orderByDesc(WithdrawalOrder::getCreateTime);
        
        // 分页查询
        Page<WithdrawalOrder> pageParam = new Page<>(page, size);
        Page<WithdrawalOrder> result = withdrawalOrderMapper.selectPage(pageParam, wrapper);
        
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
     * 根据订单号查询单笔提现订单
     * 
     * @param orderNo 订单号
     * @return 提现订单
     */
    @GetMapping("/order/{orderNo}")
    public ResponseEntity<Map<String, Object>> getWithdrawalByOrderNo(@PathVariable String orderNo) {
        log.info("查询提现订单: orderNo={}", orderNo);
        
        LambdaQueryWrapper<WithdrawalOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WithdrawalOrder::getOrderNo, orderNo);
        
        WithdrawalOrder order = withdrawalOrderMapper.selectOne(wrapper);
        
        Map<String, Object> response = new HashMap<>();
        if (order != null) {
            response.put("code", 200);
            response.put("message", "success");
            response.put("data", order);
        } else {
            response.put("code", 404);
            response.put("message", "提现订单不存在");
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 根据用户地址查询提现历史
     * 
     * @param userAddress 用户钱包地址
     * @param page 页码
     * @param size 每页大小
     * @return 提现历史列表
     */
    @GetMapping("/user/{userAddress}")
    public ResponseEntity<Map<String, Object>> getUserWithdrawals(
            @PathVariable String userAddress,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        log.info("查询用户提现历史: userAddress={}, page={}, size={}", userAddress, page, size);
        
        LambdaQueryWrapper<WithdrawalOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WithdrawalOrder::getUserAddress, userAddress);
        wrapper.orderByDesc(WithdrawalOrder::getCreateTime);
        
        Page<WithdrawalOrder> pageParam = new Page<>(page, size);
        Page<WithdrawalOrder> result = withdrawalOrderMapper.selectPage(pageParam, wrapper);
        
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
     * 统计提现数据
     * 
     * @param chainName 链名称（可选）
     * @return 统计数据
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getWithdrawalStatistics(
            @RequestParam(required = false) String chainName) {
        
        log.info("统计提现数据: chainName={}", chainName);
        
        LambdaQueryWrapper<WithdrawalOrder> wrapper = new LambdaQueryWrapper<>();
        
        if (chainName != null && !chainName.isEmpty()) {
            wrapper.eq(WithdrawalOrder::getChainName, chainName);
        }
        
        // 总提现笔数
        Long totalCount = withdrawalOrderMapper.selectCount(wrapper);
        
        // 已完成提现笔数
        LambdaQueryWrapper<WithdrawalOrder> completedWrapper = new LambdaQueryWrapper<>();
        completedWrapper.eq(WithdrawalOrder::getStatus, 2);
        if (chainName != null && !chainName.isEmpty()) {
            completedWrapper.eq(WithdrawalOrder::getChainName, chainName);
        }
        Long completedCount = withdrawalOrderMapper.selectCount(completedWrapper);
        
        // 待审批提现笔数
        LambdaQueryWrapper<WithdrawalOrder> pendingWrapper = new LambdaQueryWrapper<>();
        pendingWrapper.eq(WithdrawalOrder::getStatus, 0);
        if (chainName != null && !chainName.isEmpty()) {
            pendingWrapper.eq(WithdrawalOrder::getChainName, chainName);
        }
        Long pendingCount = withdrawalOrderMapper.selectCount(pendingWrapper);
        
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "success");
        
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("totalCount", totalCount);
        statistics.put("completedCount", completedCount);
        statistics.put("pendingCount", pendingCount);
        statistics.put("failedCount", totalCount - completedCount - pendingCount);
        
        response.put("data", statistics);
        
        return ResponseEntity.ok(response);
    }
}
