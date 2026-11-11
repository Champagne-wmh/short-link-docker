package com.shortlink.manager;

import com.shortlink.mapper.LeafAllocMapper;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 号段模式发号器（ID Generator）。
 *
 * <p>核心思想：不在每次生成 ID 时都访问数据库，而是「一次取一批、内存里慢慢发」。</p>
 *
 * <p><b>工作流程</b>：</p>
 * <ol>
 *   <li>应用启动时，内存里还没有号段（currentId == maxId）。</li>
 *   <li>第一次取 ID 时，执行 UPDATE 把数据库 max_id 增加 step（如 +1000），
 *       一次性把 [max_id-step+1, max_id] 这 1000 个 ID 装进内存。</li>
 *   <li>后续 999 次取 ID 完全走内存，零网络、零数据库开销。</li>
 *   <li>号段用完，再重复第 2 步。</li>
 * </ol>
 *
 * <p>数据库的访问频率因此降低了 step 倍，这就是它能支撑高并发的本质。</p>
 *
 * <p><b>线程安全与分布式</b>：</p>
 * <ul>
 *   <li>单机：{@code synchronized} 保证进程内线程安全。</li>
 *   <li>多实例：每个实例独立执行 UPDATE 取号段，数据库行锁 + 原子自增
 *       保证各实例拿到的号段互不重叠——这也是号段模式天然支持水平扩容的原因。</li>
 * </ul>
 */
@Component
public class IdGenerator {

    private static final String BIZ_TAG = "short_link";

    @Resource
    private LeafAllocMapper leafAllocMapper;

    /** 号段步长，可在 application.yml 里通过 short-link.id-segment-step 配置。 */
    @Value("${short-link.id-segment-step:1000}")
    private int step;

    /** 内存号段的下界（下一个待分配的 ID）。 */
    private final AtomicLong currentId = new AtomicLong(0L);

    /** 内存号段的上界（当前号段能分配到的最大 ID）。 */
    private volatile long maxId = 0L;

    /**
     * 获取下一个全局唯一 ID。
     */
    public long nextId() {
        long id = currentId.incrementAndGet();
        if (id <= maxId) return id;
        synchronized (this){
            if(currentId.get() >= maxId) loadSegment();
            return currentId.incrementAndGet();
        }
    }

    /**
     * 从数据库申请一个新号段。
     *
     * <p>两步操作：先原子自增 max_id，再读出新的 max_id，
     * 得到号段 (newMax - step, newMax]。</p>
     */
    private void loadSegment() {
        // 1. UPDATE t_leaf_alloc SET max_id = max_id + step（原子操作，加行锁）
        leafAllocMapper.updateMaxId(BIZ_TAG, step);
        // 2. 读出新的 max_id，即本号段的上界
        long newMax = leafAllocMapper.selectMaxId(BIZ_TAG);
        // 3. 号段下界 = 上界 - step
        this.currentId.set(newMax - step);
        this.maxId = newMax;
    }
}
