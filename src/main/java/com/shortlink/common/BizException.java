package com.shortlink.common;

import lombok.Getter;

/**
 * 业务异常。
 *
 * <p>code 同时充当 HTTP 状态码，方便跳转接口直接返回 404 / 403 / 410 等语义化状态码。</p>
 */
@Getter
public class BizException extends RuntimeException {

    /** 业务码 / HTTP 状态码。 */
    private final int code;

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BizException(String message) {
        this(500, message);
    }
}
