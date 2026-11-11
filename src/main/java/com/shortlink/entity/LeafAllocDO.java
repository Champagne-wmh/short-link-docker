package com.shortlink.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 号段发号器表实体，对应表 t_leaf_alloc。
 */
@Data
@TableName("t_leaf_alloc")
public class LeafAllocDO {

    /** 业务标识（如 short_link），也是主键。 */
    @TableId(type = IdType.INPUT)
    private String bizTag;

    /** 当前已分配的最大 ID。 */
    private Long maxId;

    /** 号段步长，每次从 DB 取多少 ID 放进内存。 */
    private Integer step;

    /** 描述。 */
    private String description;

    /** 更新时间。 */
    private LocalDateTime updateTime;
}
