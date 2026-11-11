package com.shortlink.service.impl;

import com.shortlink.dto.ShortLinkAccessMsg;
import com.shortlink.entity.LinkAccessLogDO;
import com.shortlink.mapper.LinkAccessLogMapper;
import com.shortlink.mapper.LinkAccessStatsMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Slf4j
@Service
public class LinkAccessService {

    @Resource
    private LinkAccessLogMapper logMapper;
    @Resource private LinkAccessStatsMapper statsMapper;

    @Transactional(rollbackFor = Exception.class)
    public void handle(ShortLinkAccessMsg msg) {
        // 1. 插明细
        LinkAccessLogDO logDO = new LinkAccessLogDO();
        logDO.setMsgId(msg.getMsgId());
        logDO.setGid(msg.getGid());
        logDO.setShortCode(msg.getShortCode());
        logDO.setIp(msg.getIp());
        logDO.setAccessTime(toLocalDateTime(msg.getAccessTime()));
        logMapper.insert(logDO);

        // 2. 按天聚合
        LocalDate date = logDO.getAccessTime().toLocalDate();
        statsMapper.upsertDaily(msg.getGid(), msg.getShortCode(), date);
    }

    private LocalDateTime toLocalDateTime(Long ts) {
        return ts == null ? LocalDateTime.now()
                : LocalDateTime.ofInstant(Instant.ofEpochMilli(ts), ZoneId.systemDefault());
    }
}
