package com.shortlink.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ShortLinkAccessMsg  implements Serializable {
    /** 消息唯一ID，幂等键。一次点击生成一个 UUID，重发不变。 */
    private String msgId;

    /** 分组ID。统计表 uk_code_date = (gid, short_code, access_date)，必须带上。 */
    private String gid;

    /** 短码。 */
    private String shortCode;

    /** 客户端 IP（已做 X-Forwarded-For 解析）。 */
    private String ip;

    /** User-Agent 原文，消费端再解析 browser/os/device。 */
    private String userAgent;

    /** 点击发生时间（毫秒时间戳），在 Controller 里取，不取消费时间。 */
    private Long accessTime;
}
