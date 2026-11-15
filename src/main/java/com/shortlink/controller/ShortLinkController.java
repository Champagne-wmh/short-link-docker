package com.shortlink.controller;

import com.shortlink.common.Result;
import com.shortlink.dto.CreateShortLinkReq;
import com.shortlink.dto.ShortLinkAccessMsg;
import com.shortlink.dto.ShortLinkResp;
import com.shortlink.entity.ShortLinkDO;
import com.shortlink.mq.Producer;
import com.shortlink.service.ShortLinkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * 短链接接口。
 *
 * <p>跳转接口挂在根路径（/{shortCode}），管理接口挂在 /api/short-link 下。</p>
 */
@Slf4j
@RestController
@Tag(name = "短链接", description = "短链生成、跳转与管理")
public class ShortLinkController {

    @Resource
    private ShortLinkService shortLinkService;
    @Resource
    private Producer producer;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 创建短链。
     */
    @Operation(summary = "创建短链", description = "提交长链接，返回短链；可选自定义短码与有效期")
    @PostMapping("/api/short-link")
    public Result<ShortLinkResp> create(@Valid @RequestBody CreateShortLinkReq req) {
        return Result.success(shortLinkService.create(req));
    }

    /**
     * 短链跳转（核心接口）。
     *
     * <p>返回 302 + Location 头。使用 302 而非 301 是为了让每次点击都回到后端，
     * 从而能够统计点击、动态修改目标、灵活下线。</p>
     */
    @Operation(summary = "短链跳转", description = "访问短链，302 重定向到原始地址")
    @GetMapping("/jump/{shortCode}")
    public void redirect(@PathVariable String shortCode, HttpServletRequest request, HttpServletResponse response) throws IOException {
        // 1. 查库（同步，必须拿到 originUrl 才能跳转）
        ShortLinkDO link = shortLinkService.resolve(shortCode);

        // 2. 组装埋点消息。msgId 用 UUID，gid 从 link 取
        ShortLinkAccessMsg msg = new ShortLinkAccessMsg(
                UUID.randomUUID().toString(),
                link.getGid(),
                shortCode,
                getClientIp(request),
                request.getHeader("User-Agent"),
                System.currentTimeMillis()
        );

        // 3. 异步发 MQ，失败降级，绝不阻塞 302
        try {
            producer.sendOrderObj(msg);
        } catch (Exception e) {
            // V1 降级：打日志。V2 可落本地磁盘队列 / 落表异步补偿
            log.error("埋点消息发送失败，shortCode={}, msgId={}", shortCode, msg.getMsgId(), e);
        }

        // 4. 立即 302
        response.setStatus(HttpServletResponse.SC_FOUND);
        response.setHeader("Location", link.getOriginUrl());
    }



    /** 取真实客户端 IP（Nginx 后必须优先 X-Forwarded-For）。 */
    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank() && !"unknown".equalsIgnoreCase(xff)) {
            return xff.split(",")[0].trim();   // 第一个是真实客户端
        }
        String xri = request.getHeader("X-Real-IP");
        if (xri != null && !xri.isBlank() && !"unknown".equalsIgnoreCase(xri)) {
            return xri;
        }
        return request.getRemoteAddr();
    }



    /**
     * 按分组查询短链列表。
     */
    @Operation(summary = "短链列表", description = "按分组查询短链列表")
    @GetMapping("/api/short-link/list")
    public Result<List<ShortLinkDO>> list(@RequestParam(defaultValue = "default") String gid) {
        return Result.success(shortLinkService.listByGid(gid));
    }

    /**
     * 启用 / 禁用短链。
     */
    @Operation(summary = "启用/禁用", description = "enableStatus: 0=启用，1=禁用")
    @PutMapping("/api/short-link/{shortCode}/status")
    public Result<Void> updateStatus(@PathVariable String shortCode,
                                     @RequestParam Integer enableStatus) {
        shortLinkService.updateStatus(shortCode, enableStatus);
        return Result.success();
    }

    /**
     * 修改目标链接。
     */
    @Operation(summary = "修改目标链接", description = "修改短链指向的原始地址")
    @PutMapping("/api/short-link/{shortCode}/url")
    public Result<Void> updateOriginUrl(@PathVariable String shortCode,
                                        @RequestParam String originUrl) {
        shortLinkService.updateOriginUrl(shortCode, originUrl);
        return Result.success();
    }

    /**
     * 逻辑删除短链。
     */
    @Operation(summary = "删除短链", description = "逻辑删除，del_flag 置 1")
    @DeleteMapping("/api/short-link/{shortCode}")
    public Result<Void> delete(@PathVariable String shortCode) {
        shortLinkService.delete(shortCode);
        return Result.success();
    }

    /**
     * 测试Redis。
     */
    @GetMapping("/api/short-link/test-redis")
    public Result<String> testRedis() {
        stringRedisTemplate.opsForValue().set("test:key", "Hello Redis from Docker!");
        String val = stringRedisTemplate.opsForValue().get("test:key");
        return Result.success("Redis 连通性测试成功，读取值: " + val);
    }
}
