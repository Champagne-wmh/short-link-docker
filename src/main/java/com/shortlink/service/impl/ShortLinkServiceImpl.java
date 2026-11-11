package com.shortlink.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shortlink.common.BizException;
import com.shortlink.dto.CreateShortLinkReq;
import com.shortlink.dto.ShortLinkResp;
import com.shortlink.entity.ShortLinkDO;
import com.shortlink.manager.IdGenerator;
import com.shortlink.mapper.ShortLinkMapper;
import com.shortlink.service.ShortLinkService;
import com.shortlink.util.Base62;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * 短链业务实现。
 */
@Service
public class ShortLinkServiceImpl implements ShortLinkService {

    /** 自动生成短码时，短码冲突（理论上不会发生）的最大重试次数。 */
    private static final int MAX_RETRY = 3;

    @Resource
    private ShortLinkMapper shortLinkMapper;

    @Resource
    private IdGenerator idGenerator;

    /** 短链域名前缀，从 application.yml 读取。 */
    @Value("${short-link.domain}")
    private String domain;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ShortLinkResp create(CreateShortLinkReq req) {
        // 1. 校验 URL 合法性
        String originUrl = req.getOriginUrl().trim();
        validateUrl(originUrl);

        // 2. 处理分组与自定义短码
        String gid = (req.getGid() == null || req.getGid().isBlank()) ? "default" : req.getGid();
        String customCode = req.getCustomCode();
        boolean isCustom = (customCode != null && !customCode.isBlank());

        // 3. 组装实体（ID 统一由发号器生成，保证主键唯一）
        ShortLinkDO link = new ShortLinkDO();
        link.setId(idGenerator.nextId());
        link.setGid(gid);
        link.setOriginUrl(originUrl);
        link.setCreateType(isCustom ? 1 : 0);
        link.setEnableStatus(0);
        link.setDelFlag(0);

        // 4. 有效期
        Integer validDateType = req.getValidDateType() == null ? 0 : req.getValidDateType();
        link.setValidDateType(validDateType);
        if (validDateType == 1) {
            if (req.getValidDate() == null) {
                throw new BizException(400, "选择自定义有效期时，必须指定过期时间");
            }
            // 把 OffsetDateTime 转成 东八区 LocalDateTime
            LocalDateTime validLocalTime = req.getValidDate()
                    .atZone(ZoneId.of("Asia/Shanghai"))
                    .toLocalDateTime();

            // 现在两个都是LocalDateTime，可以正常isBefore比较
            if (validLocalTime.isBefore(LocalDateTime.now())) {
                throw new BizException(400, "过期时间不能早于当前时间");
            }
            // 存入DO，setValidDate接收LocalDateTime，类型匹配
            link.setValidDate(validLocalTime);
        }


        // 5. 确定短码：自定义直接用，自动生成用 Base62(id)
        if (isCustom) {
            validateCustomCode(customCode);
            link.setShortCode(customCode);
            insertWithRetry(link, true);
        } else {
            link.setShortCode(Base62.encode(link.getId()));
            insertWithRetry(link, false);
        }

        // 6. 返回结果
        ShortLinkResp resp = new ShortLinkResp();
        resp.setShortCode(link.getShortCode());
        resp.setShortUrl(domain + "/jump/" + link.getShortCode());
        resp.setOriginUrl(originUrl);
        return resp;
    }

    @Override
    public ShortLinkDO resolve(String shortCode) {
        // 先查是否存在（@TableLogic 自动过滤已删除数据）
        ShortLinkDO link = getByCode(shortCode);
        // 已禁用
        if (link.getEnableStatus() != null && link.getEnableStatus() == 1) {
            throw new BizException(403, "短链已被禁用");
        }
        // 惰性过期判断：跳转时比对 valid_date，不依赖定时任务
        if (link.getValidDateType() != null && link.getValidDateType() == 1
                && link.getValidDate() != null
                && link.getValidDate().isBefore(LocalDateTime.now())) {
            throw new BizException(410, "短链已过期");
        }
        return link;
    }

    @Override
    public List<ShortLinkDO> listByGid(String gid) {
        return shortLinkMapper.selectList(
                new LambdaQueryWrapper<ShortLinkDO>()
                        .eq(ShortLinkDO::getGid, gid)
                        .orderByDesc(ShortLinkDO::getCreateTime));
    }

    @Override
    public void updateStatus(String shortCode, Integer enableStatus) {
        // 注意：这里用 getByCode 而非 resolve，否则禁用状态下无法重新启用
        ShortLinkDO link = getByCode(shortCode);
        link.setEnableStatus(enableStatus);
        shortLinkMapper.updateById(link);
    }

    @Override
    public void updateOriginUrl(String shortCode, String originUrl) {
        validateUrl(originUrl.trim());
        ShortLinkDO link = getByCode(shortCode);
        link.setOriginUrl(originUrl.trim());
        shortLinkMapper.updateById(link);
    }

    @Override
    public void delete(String shortCode) {
        ShortLinkDO link = getByCode(shortCode);
        // @TableLogic 会让 deleteById 变成 UPDATE ... SET del_flag = 1
        shortLinkMapper.deleteById(link.getId());
    }

    /**
     * 按短码查询（供管理操作用），只校验「是否存在」，
     * 不校验启用/过期状态，避免禁用后无法重新启用或编辑。
     */
    private ShortLinkDO getByCode(String shortCode) {
        ShortLinkDO link = shortLinkMapper.selectOne(
                new LambdaQueryWrapper<ShortLinkDO>()
                        .eq(ShortLinkDO::getShortCode, shortCode));
        if (link == null) {
            throw new BizException(404, "短链不存在");
        }
        return link;
    }

    /**
     * 插入并处理唯一索引冲突。
     *
     * @param link     待插入的短链
     * @param isCustom 是否为自定义短码（自定义冲突直接报错，自动生成冲突才重试）
     */
    private void insertWithRetry(ShortLinkDO link, boolean isCustom) {
        int retry = 0;
        while (true) {
            try {
                shortLinkMapper.insert(link);
                return;
            } catch (DuplicateKeyException e) {
                if (isCustom) {
                    // 自定义短码冲突：说明该短码已被占用，直接报错
                    throw new BizException(409, "短码已被占用，请换一个");
                }
                // 自动生成理论上不会冲突（发号器保证 ID 唯一），
                // 万一发生（如号段异常），重新取 ID 生成新短码重试
                if (++retry > MAX_RETRY) {
                    throw new BizException(500, "生成短码失败，请重试");
                }
                link.setId(idGenerator.nextId());
                link.setShortCode(Base62.encode(link.getId()));
            }
        }
    }

    /** 校验 URL 必须为 http/https 协议。 */
    private void validateUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new BizException(400, "链接不能为空");
        }
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            throw new BizException(400, "链接必须以 http:// 或 https:// 开头");
        }
    }

    /** 校验自定义短码：4-16 位，仅字母数字。 */
    private void validateCustomCode(String code) {
        if (code.length() < 4 || code.length() > 16) {
            throw new BizException(400, "自定义短码长度须为 4-16 位");
        }
        if (!code.matches("[A-Za-z0-9]+")) {
            throw new BizException(400, "自定义短码只能包含字母和数字");
        }
    }
}
