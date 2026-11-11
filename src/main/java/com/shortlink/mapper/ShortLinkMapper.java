package com.shortlink.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shortlink.entity.ShortLinkDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 短链表 Mapper。
 *
 * <p>继承 BaseMapper 后自动获得单表 CRUD 能力（insert/selectById/selectOne/updateById/deleteById...），
 * 无需手写 XML。复杂查询再单独写方法。</p>
 */
@Mapper
public interface ShortLinkMapper extends BaseMapper<ShortLinkDO> {
}
