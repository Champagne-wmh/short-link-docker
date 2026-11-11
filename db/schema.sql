-- =============================================================
-- 短链接系统 数据库初始化脚本 (V1)
-- 数据库: short_link
-- 执行方式: mysql -uroot -p123456 < schema.sql
-- =============================================================

CREATE DATABASE IF NOT EXISTS short_link DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE short_link;

-- -------------------------------------------------------------
-- 1. 短链主表
--    uk_short_code 是短码唯一性的最后一道防线（数据库层兜底）
--    idx_gid 支撑按分组查询（也是未来分库分表的分片键）
-- -------------------------------------------------------------
DROP TABLE IF EXISTS t_short_link;
CREATE TABLE t_short_link (
  id              BIGINT        NOT NULL                COMMENT '主键ID（发号器生成）',
  gid             VARCHAR(32)   NOT NULL DEFAULT 'default' COMMENT '分组ID(未来分片键)',
  short_code      VARCHAR(16)   NOT NULL                COMMENT '短码(全局唯一)',
  origin_url      VARCHAR(2048) NOT NULL                COMMENT '原始长链接',
  create_type     TINYINT       NOT NULL DEFAULT 0      COMMENT '0=自动生成 1=自定义短码',
  valid_date_type TINYINT       NOT NULL DEFAULT 0      COMMENT '0=永久有效 1=自定义到期',
  valid_date      DATETIME      NULL                    COMMENT '过期时间(valid_date_type=1时生效)',
  enable_status   TINYINT       NOT NULL DEFAULT 0      COMMENT '0=启用 1=禁用',
  del_flag        TINYINT       NOT NULL DEFAULT 0      COMMENT '0=正常 1=已删除(逻辑删除)',
  create_time     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_short_code (short_code),
  KEY idx_gid (gid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='短链接主表';

-- -------------------------------------------------------------
-- 2. 号段模式发号器表
--    原理: 应用启动/号段耗尽时执行
--          UPDATE t_leaf_alloc SET max_id = max_id + step WHERE biz_tag = 'short_link'
--    一次性把 step 个 ID 装载进内存，DB 访问量降低 step 倍。
--    初始 max_id = 1 亿，让 Base62 编码后的短码从 5 位起步，避免短码过短。
-- -------------------------------------------------------------
DROP TABLE IF EXISTS t_leaf_alloc;
CREATE TABLE t_leaf_alloc (
  biz_tag     VARCHAR(64) NOT NULL COMMENT '业务标识(如 short_link)',
  max_id      BIGINT      NOT NULL DEFAULT 0 COMMENT '当前已分配的最大ID',
  step        INT         NOT NULL DEFAULT 1000 COMMENT '号段步长',
  description VARCHAR(128) COMMENT '描述',
  update_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (biz_tag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='号段模式发号器';

INSERT INTO t_leaf_alloc (biz_tag, max_id, step, description)
VALUES ('short_link', 100000000, 1000, '短链接ID号段');

-- -------------------------------------------------------------
-- 3. 点击访问明细表 (V2 统计功能使用, V1 先建好结构)
--    数据量巨大时按 access_time 做 RANGE 分区, 可 DROP PARTITION 秒级清理
-- -------------------------------------------------------------
DROP TABLE IF EXISTS t_link_access_logs;
CREATE TABLE t_link_access_logs (
  id          BIGINT       NOT NULL              COMMENT '主键ID',
  gid         VARCHAR(32)  NOT NULL              COMMENT '分组ID',
  short_code  VARCHAR(16)  NOT NULL              COMMENT '短码',
  access_time DATETIME     NOT NULL              COMMENT '访问时间',
  ip          VARCHAR(64)                        COMMENT '客户端IP',
  browser     VARCHAR(64)                        COMMENT '浏览器',
  os          VARCHAR(64)                        COMMENT '操作系统',
  device      VARCHAR(64)                        COMMENT '设备类型',
  referer     VARCHAR(512)                       COMMENT '来源页',
  country     VARCHAR(64)                        COMMENT '国家',
  city        VARCHAR(64)                        COMMENT '城市',
  uv          VARCHAR(64)                        COMMENT '访客标识(IP+UA哈希)',
  PRIMARY KEY (id),
  KEY idx_code_time (short_code, access_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='短链访问明细';

-- -------------------------------------------------------------
-- 4. 按天聚合统计表 (V2 使用)
--    消费端用 INSERT ... ON DUPLICATE KEY UPDATE pv = pv + 1 实现幂等累加
-- -------------------------------------------------------------
DROP TABLE IF EXISTS t_link_access_stats;
CREATE TABLE t_link_access_stats (
  id          BIGINT       NOT NULL              COMMENT '主键ID',
  gid         VARCHAR(32)  NOT NULL              COMMENT '分组ID',
  short_code  VARCHAR(16)  NOT NULL              COMMENT '短码',
  access_date DATE         NOT NULL              COMMENT '统计日期',
  pv          INT          NOT NULL DEFAULT 0    COMMENT '页面访问量',
  uv          INT          NOT NULL DEFAULT 0    COMMENT '独立访客数',
  uip         INT          NOT NULL DEFAULT 0    COMMENT '独立IP数',
  hour_pv     VARCHAR(512)                       COMMENT '24小时PV分布(JSON)',
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_code_date (gid, short_code, access_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='短链访问统计(按天)';
