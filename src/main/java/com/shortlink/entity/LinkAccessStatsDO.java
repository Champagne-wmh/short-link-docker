package com.shortlink.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;

@Data
@TableName("t_link_access_stats")
public class LinkAccessStatsDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String gid;
    private String shortCode;
    private LocalDate accessDate;
    private Integer pv;
    private Integer uv;
    private Integer uip;
}
