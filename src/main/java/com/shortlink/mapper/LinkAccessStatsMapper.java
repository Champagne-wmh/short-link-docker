package com.shortlink.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shortlink.entity.LinkAccessStatsDO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;

@Mapper
public interface LinkAccessStatsMapper extends BaseMapper<LinkAccessStatsDO> {

    // 把你刚才写的那段 SQL 粘过来
    @Insert("""
            INSERT INTO t_link_access_stats (gid, short_code, access_date, pv, uv, uip)
            VALUES (#{gid}, #{shortCode}, #{accessDate}, 1, 1, 1)
            ON DUPLICATE KEY UPDATE
                pv = pv + 1,
                uv = uv + 1,
                uip = uip + 1,
                update_time = CURRENT_TIMESTAMP
            """)
    int upsertDaily(@Param("gid") String gid,
                    @Param("shortCode") String shortCode,
                    @Param("accessDate") LocalDate accessDate);
}
