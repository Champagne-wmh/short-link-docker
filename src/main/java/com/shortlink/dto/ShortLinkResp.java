package com.shortlink.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 短链响应体。
 */
@Data
@Schema(description = "短链响应")
public class ShortLinkResp {

    @Schema(description = "短码", example = "1V6K3")
    private String shortCode;

    @Schema(description = "完整短链", example = "http://localhost:8080/1V6K3")
    private String shortUrl;

    @Schema(description = "原始链接")
    private String originUrl;
}
