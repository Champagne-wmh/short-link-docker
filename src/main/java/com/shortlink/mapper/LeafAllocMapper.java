package com.shortlink.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shortlink.entity.LeafAllocDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 号段发号器表 Mapper。
 *
 * <p>这里演示了注解形式的自定义 SQL：号段模式的核心就是这两条语句。</p>
 */
@Mapper
public interface LeafAllocMapper extends BaseMapper<LeafAllocDO> {

    /**
     * 原子地申请一个号段：把 max_id 增加 step。
     *
     * <p>这条 UPDATE 会加行锁，保证并发下每个请求拿到的号段不重叠。</p>
     */
    @Update("UPDATE t_leaf_alloc SET max_id = max_id + #{step} WHERE biz_tag = #{bizTag}")
    int updateMaxId(@Param("bizTag") String bizTag, @Param("step") int step);

    /**
     * 查询当前已分配到的最大 ID（号段上限）。
     */
    @Select("SELECT max_id FROM t_leaf_alloc WHERE biz_tag = #{bizTag}")
    Long selectMaxId(@Param("bizTag") String bizTag);
}
