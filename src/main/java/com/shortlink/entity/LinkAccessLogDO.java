package com.shortlink.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_link_access_logs")
public class LinkAccessLogDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String msgId;
    private String gid;
    private String shortCode;
    private LocalDateTime accessTime;
    private String ip;
    // 其他字段可以暂时不填
}
