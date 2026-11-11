package com.shortlink.util;

/**
 * Base62 编解码工具。
 *
 * <p>把数字 ID 转换成短字符串，用于生成短码。</p>
 * <ul>
 *   <li>字符集：0-9（10 个）+ A-Z（26 个）+ a-z（26 个）= 62 个字符。</li>
 *   <li>相比 Base64，去掉了 + / 等 URL 不友好字符，短码可以直接放在 URL 路径里。</li>
 *   <li>10 亿（10^9）以内的数字，Base62 编码后约 5~6 位。</li>
 * </ul>
 *
 * <p>进制转换思想：十进制 -> 62 进制，本质是反复取余 + 整除。</p>
 */
public final class Base62 {

    /** 62 进制字符表，顺序不可随意改变（编码与解码必须一致）。 */
    private static final char[] CHARS =
            "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();

    private static final int BASE = 62;

    private Base62() {
    }

    /**
     * 数字 ID -> 短码。
     *
     * @param id 非负整数
     * @return Base62 字符串
     */
    public static String encode(long id) {
        if (id < 0) {
            throw new IllegalArgumentException("id 不能为负数: " + id);
        }
        if (id == 0) {
            return String.valueOf(CHARS[0]);
        }
        StringBuilder sb = new StringBuilder();
        while (id > 0) {
            int remainder = (int) (id % BASE);
            sb.append(CHARS[remainder]);
            id /= BASE;
        }
        // 上面是逆序拼接，最后反转
        return sb.reverse().toString();
    }

    /**
     * 短码 -> 数字 ID（encode 的逆运算）。
     *
     * @param code Base62 字符串
     * @return 原始数字
     */
    public static long decode(String code) {
        if (code == null || code.isEmpty()) {
            throw new IllegalArgumentException("code 不能为空");
        }
        long result = 0;
        for (int i = 0; i < code.length(); i++) {
            char c = code.charAt(i);
            result = result * BASE + indexOf(c);
        }
        return result;
    }

    /** 返回字符在字符表中的下标。 */
    private static int indexOf(char c) {
        if (c >= '0' && c <= '9') {
            return c - '0';
        }
        if (c >= 'A' && c <= 'Z') {
            return c - 'A' + 10;
        }
        if (c >= 'a' && c <= 'z') {
            return c - 'a' + 36;
        }
        throw new IllegalArgumentException("非法 Base62 字符: " + c);
    }
}
