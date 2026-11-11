package com.shortlink.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 短链接实体，对应表 t_short_link。
 *
 * <p>字段命名采用驼峰，MyBatis-Plus 开启 map-underscore-to-camel-case 后
 * 会自动映射到数据库下划线字段（如 shortCode <-> short_code）。</p>
 */
@Data
@TableName("t_short_link")
public class ShortLinkDO {

    /**
     * 主键 ID，由号段发号器生成（不是数据库自增）。
     * IdType.INPUT 表示由程序赋值，MyBatis-Plus 不做自增处理。
     */
    @TableId(type = IdType.INPUT)
    private Long id;

    /** 分组 ID，默认 default，未来分库分表的分片键。 */
    private String gid;

    /** 短码，全局唯一，由唯一索引 uk_short_code 兜底。 */
    private String shortCode;

    /** 原始长链接。 */
    private String originUrl;

    /** 创建类型：0=自动生成，1=自定义短码。 */
    private Integer createType;

    /** 有效期类型：0=永久有效，1=自定义到期时间。 */
    private Integer validDateType;

    /** 过期时间，validDateType=1 时生效。 */
    private LocalDateTime validDate;

    /** 启用状态：0=启用，1=禁用。 */
    private Integer enableStatus;

    /**
     * 逻辑删除标记：0=正常，1=已删除。
     * @TableLogic 让 MyBatis-Plus 自动在查询/更新时追加 del_flag=0 条件，
     * 删除时自动改成 UPDATE 而不是物理 DELETE。
     */
    @TableLogic
    private Integer delFlag;

    /** 创建时间（数据库 DEFAULT CURRENT_TIMESTAMP 填充）。 */
    private LocalDateTime createTime;

    /** 更新时间（数据库 ON UPDATE CURRENT_TIMESTAMP 填充）。 */
    private LocalDateTime updateTime;
}
