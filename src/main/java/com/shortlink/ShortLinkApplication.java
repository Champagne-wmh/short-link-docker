package com.shortlink;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 短链接系统启动类。
 *
 * <p>@MapperScan 扫描 Mapper 接口所在的包，MyBatis-Plus 会为它们生成实现类。</p>
 */
@SpringBootApplication
@MapperScan("com.shortlink.mapper")
public class ShortLinkApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShortLinkApplication.class, args);
    }
}
