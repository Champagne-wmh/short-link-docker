package com.shortlink.common;

import lombok.Data;

/**
 * 统一响应结构。
 *
 * <p>所有接口都返回这个结构，前端无需判断各种返回格式。</p>
 *
 * @param <T> 数据体类型
 */
@Data
public class Result<T> {

    /** 业务状态码：0 表示成功，非 0 表示失败。 */
    private Integer code;

    /** 提示信息。 */
    private String message;

    /** 数据体。 */
    private T data;

    private Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(0, "success", data);
    }

    public static <T> Result<T> success() {
        return new Result<>(0, "success", null);
    }

    public static <T> Result<T> fail(Integer code, String message) {
        return new Result<>(code, message, null);
    }
}
