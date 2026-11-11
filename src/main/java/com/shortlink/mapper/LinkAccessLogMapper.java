package com.shortlink.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shortlink.entity.LinkAccessLogDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LinkAccessLogMapper extends BaseMapper<LinkAccessLogDO> {
    // 直接继承 BaseMapper 就有 insert 方法了
}
