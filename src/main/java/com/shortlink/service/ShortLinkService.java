package com.shortlink.service;

import com.shortlink.dto.CreateShortLinkReq;
import com.shortlink.dto.ShortLinkResp;
import com.shortlink.entity.ShortLinkDO;

import java.util.List;

/**
 * 短链业务接口。
 */
public interface ShortLinkService {

    /**
     * 创建短链（支持自动生成与自定义短码）。
     */
    ShortLinkResp create(CreateShortLinkReq req);

    /**
     * 跳转解析：根据短码找到原始链接。
     *
     * <p>短码不存在抛 404、已禁用抛 403、已过期抛 410。</p>
     */
    ShortLinkDO resolve(String shortCode);

    /**
     * 按分组查询短链列表。
     */
    List<ShortLinkDO> listByGid(String gid);

    /**
     * 启用 / 禁用短链。
     */
    void updateStatus(String shortCode, Integer enableStatus);

    /**
     * 修改目标链接。
     */
    void updateOriginUrl(String shortCode, String originUrl);

    /**
     * 逻辑删除短链。
     */
    void delete(String shortCode);
}
