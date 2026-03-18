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
}
