package com.xyqb.platform.common.constant;

/**
 * 应用全局常量定义
 *
 * <p>集中管理项目中所有硬编码的常量值，避免散落在各处的魔法值。
 * 使用 {@code final} 类 + 私有构造器确保此类不可被实例化和继承。
 *
 * <p>常量命名规范：全大写，单词间用下划线分隔（UPPER_SNAKE_CASE）。
 *
 * @author XYQB Team
 * @since 1.0.0
 */
public final class AppConstants {

    /** 私有构造器，防止实例化 */
    private AppConstants() {
    }

    /** RESTful API 统一路径前缀，所有接口 URL 都以此开头 */
    public static final String API_PREFIX = "/api/v1";

    /** 分类树最大允许层级（从 0 开始计数，5 表示最多 6 层） */
    public static final int MAX_CATEGORY_LEVEL = 5;

    /** 分页查询默认每页条数 */
    public static final int DEFAULT_PAGE_SIZE = 20;

    /** 分页查询允许的最大每页条数（防止一次查询过多数据） */
    public static final int MAX_PAGE_SIZE = 100;

    // ==================== Redis 缓存 Key ====================
    //
    // Redis Key 的命名约定：用冒号（:）分隔层级，格式为 "模块:资源:标识"
    // 类比前端：就像前端 localStorage 的 key 命名 "app:user:token"
    // 冒号在 Redis 中是约定俗成的分隔符，Redis 客户端工具（如 RedisInsight）
    // 会自动按冒号分组展示，形成树形结构，方便查看和管理

    /**
     * 分类全量列表的缓存 Key
     *
     * <p>
     * 存储内容：所有分类的 {@code List<Category>}，JSON 格式
     * 使用场景：{@code getCategoryTree()} 方法先从这个 Key 读取缓存
     * </p>
     */
    public static final String CACHE_KEY_CATEGORY_LIST = "category:list";

    /**
     * 分类详情的缓存 Key 前缀
     *
     * <p>
     * 使用时拼接分类 ID，例如：{@code "category:detail:" + "507f1f77bcf86cd799439011"}
     * 最终 Key 为：{@code "category:detail:507f1f77bcf86cd799439011"}
     * </p>
     */
    public static final String CACHE_KEY_CATEGORY_DETAIL_PREFIX = "category:detail:";

    /**
     * 分类缓存的默认过期时间（单位：分钟）
     *
     * <p>
     * 为什么要设置过期时间（TTL = Time To Live）？
     * <ul>
     *     <li>防止缓存和数据库数据长时间不一致（即使忘记清缓存，30 分钟后也会自动过期）</li>
     *     <li>防止 Redis 内存无限增长</li>
     *     <li>30 分钟是一个平衡点：既减少了数据库查询，又不会让数据太旧</li>
     * </ul>
     * </p>
     */
    public static final long CACHE_CATEGORY_TTL_MINUTES = 30;

    /**
     * 空结果缓存的过期时间（单位：分钟）
     *
     * <p>
     * 当数据库查询结果为空时，用较短的 TTL 缓存空结果，防止缓存穿透
     * （即同一个不存在的数据被反复请求，每次都穿透到数据库）。
     * 5 分钟后自动过期，届时会重新查库确认是否已有新数据。
     * </p>
     */
    public static final long CACHE_EMPTY_TTL_MINUTES = 5;
}
