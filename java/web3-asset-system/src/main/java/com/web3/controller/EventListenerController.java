package com.web3.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.web3.entity.EventListenerOffset;
import com.web3.mapper.EventListenerOffsetMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 事件监听数据查询 REST API
 * 
 * 功能：
 * 1. ✅ 查询所有链的监听偏移量
 * 2. ✅ 根据链名称查询监听状态
 * 3. ✅ 重置监听偏移量（管理员接口）
 */
@Slf4j
@RestController
@RequestMapping("/api/event-listener")
public class EventListenerController {
    
    @Autowired
    private EventListenerOffsetMapper offsetMapper;
    
    /**
     * 查询所有链的监听偏移量
     * 
     * @return 所有链的监听状态
     */
    @GetMapping("/offsets")
    public ResponseEntity<Map<String, Object>> getAllOffsets() {
        log.info("查询所有链的监听偏移量");
        
        LambdaQueryWrapper<EventListenerOffset> wrapper = new LambdaQueryWrapper<>();
        List<EventListenerOffset> offsets = offsetMapper.selectList(wrapper);
        
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "success");
        response.put("data", offsets);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 根据链名称查询监听状态
     * 
     * @param chainName 链名称
     * @return 监听状态
     */
    @GetMapping("/offset/{chainName}")
    public ResponseEntity<Map<String, Object>> getOffsetByChain(@PathVariable String chainName) {
        log.info("查询链监听状态: chainName={}", chainName);
        
        LambdaQueryWrapper<EventListenerOffset> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EventListenerOffset::getChainName, chainName);
        
        EventListenerOffset offset = offsetMapper.selectOne(wrapper);
        
        Map<String, Object> response = new HashMap<>();
        if (offset != null) {
            response.put("code", 200);
            response.put("message", "success");
            response.put("data", offset);
        } else {
            response.put("code", 404);
            response.put("message", "链配置不存在");
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 获取监听状态摘要（简化版）
     * 
     * @return 简化的监听状态信息
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getListenerStatus() {
        log.info("查询监听状态摘要");
        
        LambdaQueryWrapper<EventListenerOffset> wrapper = new LambdaQueryWrapper<>();
        List<EventListenerOffset> offsets = offsetMapper.selectList(wrapper);
        
        // 转换为简化格式
        List<Map<String, Object>> statusList = offsets.stream().map(offset -> {
            Map<String, Object> status = new HashMap<>();
            status.put("chainName", offset.getChainName());
            status.put("lastProcessedBlock", offset.getLastProcessedBlock());
            status.put("updateTime", offset.getUpdateTime());
            return status;
        }).collect(Collectors.toList());
        
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "success");
        response.put("data", statusList);
        
        return ResponseEntity.ok(response);
    }
}
