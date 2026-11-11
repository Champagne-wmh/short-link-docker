package com.shortlink.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

/**
 * 创建短链请求参数。
 */
@Data
@Schema(description = "创建短链请求")
public class CreateShortLinkReq {

    @Schema(description = "原始长链接", example = "https://www.example.com/very/long/url")
    @NotBlank(message = "原始链接不能为空")
    private String originUrl;

    @Schema(description = "分组 ID", example = "default")
    private String gid = "default";

    @Schema(description = "有效期类型：0=永久，1=自定义", example = "0")
    private Integer validDateType = 0;

    @Schema(description = "过期时间（validDateType=1 时必填）", example = "2026-12-31 23:59:59")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime validDate;

    @Schema(description = "自定义短码（可选，留空则自动生成）", example = "wuhan2027")
    private String customCode;
}
