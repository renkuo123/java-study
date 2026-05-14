package com.xyqb.platform.module.category.service.impl;

import com.xyqb.platform.common.constant.AppConstants;
import com.xyqb.platform.common.enums.BusinessCode;
import com.xyqb.platform.common.exception.BusinessException;
import com.xyqb.platform.common.service.RedisCacheService;
import com.xyqb.platform.common.vo.PageResult;
import com.xyqb.platform.module.category.dto.CategoryCreateDTO;
import com.xyqb.platform.module.category.dto.CategoryUpdateDTO;
import com.xyqb.platform.module.category.entity.Category;
import com.xyqb.platform.module.category.repository.CategoryRepository;
import com.xyqb.platform.module.category.service.CategoryService;
import com.xyqb.platform.module.category.vo.CategoryVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 分类管理服务实现类
 *
 * <p>
 * 实现 {@link CategoryService} 接口，处理分类模块的所有业务逻辑。
 *
 * <p>
 * 核心设计：
 * <ul>
 * <li>通过构造器注入 Repository 和 RedisCacheService（{@code @RequiredArgsConstructor}），满足依赖注入最佳实践</li>
 * <li>所有数据库实体（Entity）在 Service 层转换为视图对象（VO）后返回，Controller 不直接接触 Entity</li>
 * <li>业务校验失败统一抛出 {@link BusinessException}，由全局异常处理器转换为标准响应</li>
 * <li><b>Redis 缓存</b>：使用 Cache-Aside（旁路缓存）模式减少 MongoDB 查询压力</li>
 * </ul>
 *
 * <h3>Cache-Aside（旁路缓存）模式流程图</h3>
 * <pre>
 * 读操作（getCategoryTree / getCategoryById）：
 * ┌──────────┐    1.查缓存     ┌───────┐
 * │  调用方   │ ──────────────→ │ Redis │
 * │          │                 │       │
 * │          │ ←── 2a.命中 ──── │       │    → 直接返回，不查数据库（快！）
 * │          │                 └───────┘
 * │          │     2b.未命中
 * │          │ ──────────────→ ┌─────────┐
 * │          │   3.查数据库    │ MongoDB │
 * │          │ ←── 4.返回 ──── │         │
 * │          │                 └─────────┘
 * │          │ ── 5.写缓存 ──→ ┌───────┐
 * │          │                 │ Redis │   → 下次就能命中了
 * └──────────┘                 └───────┘
 *
 * 写操作（create / update / delete）：
 * ┌──────────┐    1.写数据库    ┌─────────┐
 * │  调用方   │ ──────────────→ │ MongoDB │
 * │          │ ←── 2.完成 ──── │         │
 * │          │                 └─────────┘
 * │          │ ── 3.删缓存 ──→ ┌───────┐
 * │          │                 │ Redis │   → 让下次读取重新从 DB 加载最新数据
 * └──────────┘                 └───────┘
 *
 * 为什么写操作时是"删除"缓存而不是"更新"缓存？
 * → 因为"删除"更安全：如果两个请求并发更新，缓存可能写入旧数据（竞态条件）；
 *   而"删除"后下次读取自然会从 DB 拿到最新数据，避免了并发问题。
 * → 类比前端：React Query 的 invalidateQueries() 也是让缓存失效而非直接更新。
 * </pre>
 *
 * <p>
 * 树形结构组装算法：
 * <ol>
 * <li>一次性查询所有分类（适用于分类总量不大的场景，避免递归查询 N+1 问题）</li>
 * <li>按 order 字段排序后返回扁平列表</li>
 * </ol>
 *
 * @author XYQB Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor  // Lombok 自动生成构造函数，包含下面两个 final 字段
public class CategoryServiceImpl implements CategoryService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    // ====== 依赖注入 ======
    // @RequiredArgsConstructor 会自动生成如下构造函数：
    // public CategoryServiceImpl(CategoryRepository categoryRepository, RedisCacheService redisCacheService) {
    //     this.categoryRepository = categoryRepository;
    //     this.redisCacheService = redisCacheService;
    // }
    // Spring 启动时会自动调用这个构造函数，把对应的 Bean 传进来

    /** MongoDB 分类数据仓库 */
    private final CategoryRepository categoryRepository;

    /**
     * Redis 缓存服务
     *
     * <p>
     * 新增的依赖：通过 @RequiredArgsConstructor 自动注入。
     * 只要在类中声明 private final 字段，Lombok 就会在构造函数中添加这个参数，
     * Spring 会自动找到对应类型的 Bean（我们在 RedisCacheService 上标了 @Service）注入进来。
     * 类比前端：就像 React 组件的 props 自动传递，或 Vue 的 inject 自动获取。
     * </p>
     */
    private final RedisCacheService redisCacheService;

    /**
     * {@inheritDoc}
     *
     * <p>
     * 实现策略：
     * <ol>
     *     <li><b>先查 Redis 缓存</b>：尝试从缓存中获取全量分类列表</li>
     *     <li><b>缓存未命中则查 MongoDB</b>：查询结果写入缓存供后续使用</li>
     *     <li><b>内存中过滤、排序、分页</b>：在查到的全量数据上做处理</li>
     * </ol>
     * 缓存的是全量原始数据（未过滤/排序/分页），因为：
     * <ul>
     *     <li>分类总量小（万级以下），全量缓存内存占用可控</li>
     *     <li>分页/过滤/排序的参数组合太多，缓存每种组合会导致 Key 爆炸</li>
     * </ul>
     */
    @Override
    public PageResult<CategoryVO> getCategoryTree(int pageNo, int pageSize, String nameKeyword) {
        // ====== 参数规范化 ======
        // 三元运算符 condition ? a : b — Java 和 JS 语法完全一样
        int pn = pageNo < 1 ? 1 : pageNo;
        // Math.min(a, b) 取较小值，防止 pageSize 超过上限
        int ps = pageSize < 1 ? DEFAULT_PAGE_SIZE : Math.min(pageSize, MAX_PAGE_SIZE);

        // ====== Cache-Aside 读取策略 ======
        // 第一步：尝试从 Redis 缓存中获取全量分类列表
        List<Category> allCategories = redisCacheService.getList(
                AppConstants.CACHE_KEY_CATEGORY_LIST,  // Key = "category:list"
                Category.class                          // 告诉缓存服务：列表中的元素类型是 Category
        );

        // 第二步：判断缓存是否命中
        if (allCategories == null) {
            // 缓存未命中（null 表示 Redis 中没有这个 Key，或 Redis 异常降级返回了 null）
            log.debug("分类列表缓存未命中，查询 MongoDB");
            allCategories = categoryRepository.findAll();

            // 第三步：查到数据后写入缓存
            // 无论结果是否为空都写入缓存，防止缓存穿透
            // （缓存穿透：数据库中没有对应数据，但每次请求都穿透缓存打到数据库）
            // 空结果用较短 TTL（5 分钟），减少数据库压力的同时保证新增数据能较快生效；
            // 非空结果用正常 TTL（30 分钟）。
            // 写操作（create/update/delete）会主动清除此缓存，所以不会出现数据不一致。
            long ttl = allCategories.isEmpty()
                    ? AppConstants.CACHE_EMPTY_TTL_MINUTES
                    : AppConstants.CACHE_CATEGORY_TTL_MINUTES;
            redisCacheService.set(
                    AppConstants.CACHE_KEY_CATEGORY_LIST,       // Key
                    allCategories,                               // Value（整个列表作为 JSON 存入）
                    ttl,                                         // 过期时间
                    TimeUnit.MINUTES                             // 时间单位
            );
            log.debug("分类列表已写入缓存，共 {} 条，TTL={} 分钟", allCategories.size(), ttl);
        } else {
            log.debug("分类列表缓存命中，共 {} 条", allCategories.size());
        }

        // ====== 以下是原有的过滤、排序、分页逻辑，与缓存无关 ======

        if (allCategories.isEmpty()) {
            return PageResult.empty(pn, ps);
        }

        // 关键词过滤
        String kw = nameKeyword == null ? "" : nameKeyword.trim();
        List<Category> filtered = allCategories;
        if (!kw.isEmpty()) {
            String kwLower = kw.toLowerCase(Locale.ROOT);
            filtered = allCategories.stream()
                    .filter(c -> displayNameContainsKeyword(c.resolvedDisplayName(), kwLower))
                    .collect(Collectors.toList());
        }

        // suggestType 集合并不是树结构，这里直接按 order 字段排序后分页返回扁平列表，
        // 避免依赖历史遗留的层级字段。
        List<CategoryVO> sorted = filtered.stream()
                .sorted(Comparator.comparingInt((Category c) -> c.getOrder() == null ? 0 : c.getOrder()).reversed())
                .map(this::toVO)
                .collect(Collectors.toList());

        long total = sorted.size();
        int from = (pn - 1) * ps;
        if (from >= total) {
            return PageResult.<CategoryVO>builder()
                    .list(Collections.emptyList())
                    .total(total)
                    .pageNo(pn)
                    .pageSize(ps)
                    .totalPages((int) Math.ceil((double) total / ps))
                    .build();
        }
        int to = Math.min(from + ps, (int) total);
        // 独立 List，避免 subList 视图在序列化等场景下的歧义
        List<CategoryVO> page = new ArrayList<>(sorted.subList(from, to));

        return PageResult.<CategoryVO>builder()
                .list(page)
                .total(total)
                .pageNo(pn)
                .pageSize(ps)
                .totalPages((int) Math.ceil((double) total / ps))
                .build();
    }

    /** 展示名是否包含关键词（子串）；英文按小写比较 */
    private static boolean displayNameContainsKeyword(String displayName, String keywordLower) {
        if (displayName == null || displayName.isEmpty()) {
            return false;
        }
        return displayName.toLowerCase(Locale.ROOT).contains(keywordLower);
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * 缓存策略：先查 Redis 详情缓存 → 未命中则查 MongoDB 并回填缓存。
     * </p>
     */
    @Override
    public CategoryVO getCategoryById(String id) {
        // ====== Cache-Aside 读取 — 按 ID 查询单个分类 ======

        // 拼接缓存 Key：前缀 + ID，如 "category:detail:507f1f77bcf86cd799439011"
        // Java 的字符串拼接用 + 运算符（和 JS 一样），或者用 String.format() / StringBuilder
        String cacheKey = AppConstants.CACHE_KEY_CATEGORY_DETAIL_PREFIX + id;

        // 先查缓存
        Category cached = redisCacheService.get(cacheKey, Category.class);
        if (cached != null) {
            log.debug("分类详情缓存命中: id={}", id);
            return toVO(cached);
        }

        // 缓存未命中，查 MongoDB
        log.debug("分类详情缓存未命中，查询 MongoDB: id={}", id);
        Category category = categoryRepository.findById(id)
                // orElseThrow() 是 Optional 的方法：如果值存在就返回，不存在就抛异常
                // Optional 是 Java 8 引入的容器类，用来安全地处理可能为 null 的值
                // 类比前端：类似 TypeScript 的非空断言 value!，但更安全——它会真的检查并抛异常
                // () -> new BusinessException(...) 是 Lambda 表达式（匿名函数）
                // 类比 JS 的箭头函数：() => new BusinessException(...)
                .orElseThrow(() -> new BusinessException(BusinessCode.CATEGORY_NOT_FOUND));

        // 查到后写入缓存
        redisCacheService.set(
                cacheKey,
                category,
                AppConstants.CACHE_CATEGORY_TTL_MINUTES,
                TimeUnit.MINUTES
        );

        return toVO(category);
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * 缓存策略：创建成功后删除列表缓存，让下次查询重新从 DB 加载最新数据。
     * </p>
     */
    @Override
    public CategoryVO createCategory(CategoryCreateDTO dto) {
        // 只有一级分类：一次性读取现有数据，用于名称去重 + 计算 order
        List<Category> existing = categoryRepository.findAll();
        String newName = normalizeName(dto.getCategoryName());

        // 仅与非空展示名比较；历史空文档的 "" 不与新名称冲突，避免误报「已存在」
        if (!newName.isEmpty()
                && existing.stream()
                        .anyMatch(c -> {
                            String r = c.resolvedDisplayName();
                            return !r.isEmpty() && newName.equals(r);
                        })) {
            throw new BusinessException(BusinessCode.CATEGORY_NAME_DUPLICATE);
        }

        // DTO → Entity
        Category category = new Category();
        category.setCategoryName(newName);
        category.setLegacyCategoryName(null);
        int maxOrder = existing.stream()
                .map(Category::getOrder)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(0);
        category.setOrder(maxOrder + 1);

        Category saved = categoryRepository.save(category);
        log.info("分类已创建: id={}, name={}", saved.getId(), saved.getCategoryName());

        // ====== 清除列表缓存 ======
        // 新建了一条数据，列表缓存中没有它，所以要让列表缓存失效
        // 下次查列表时会从 MongoDB 重新加载（包含新数据）
        // 不需要清除 detail 缓存，因为新 ID 还没有被缓存过
        redisCacheService.delete(AppConstants.CACHE_KEY_CATEGORY_LIST);

        return toVO(saved);
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * 缓存策略：更新成功后删除列表缓存和该 ID 的详情缓存。
     * </p>
     */
    @Override
    public CategoryVO updateCategory(String id, CategoryUpdateDTO dto) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new BusinessException(BusinessCode.CATEGORY_NOT_FOUND));

        String newName = normalizeName(dto.getCategoryName());
        String oldName = category.resolvedDisplayName();

        // 改成与其它分类重名 → 拒绝；名称相对本条未变 → 允许（含历史字段与 name 展示一致）
        if (!newName.equals(oldName) && existsResolvedNameAmongOthers(newName, id)) {
            throw new BusinessException(BusinessCode.CATEGORY_NAME_DUPLICATE);
        }

        category.setCategoryName(newName);
        category.setLegacyCategoryName(null);
        Category updated = categoryRepository.save(category);
        log.info("分类已更新: id={}, name={}", updated.getId(), updated.getCategoryName());

        // ====== 清除缓存 ======
        // 更新后需要清两个缓存：
        // 1. 列表缓存 — 因为列表中的这条数据已经变了
        // 2. 该 ID 的详情缓存 — 如果之前有人查过这条详情，缓存中还是旧数据
        redisCacheService.delete(AppConstants.CACHE_KEY_CATEGORY_LIST);
        redisCacheService.delete(AppConstants.CACHE_KEY_CATEGORY_DETAIL_PREFIX + id);

        return toVO(updated);
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * 缓存策略：删除成功后清除列表缓存和该 ID 的详情缓存。
     * </p>
     */
    @Override
    public void deleteCategory(String id) {
        if (!categoryRepository.existsById(id)) {
            throw new BusinessException(BusinessCode.CATEGORY_NOT_FOUND);
        }

        categoryRepository.deleteById(id);
        log.info("分类已删除: id={}", id);

        // ====== 清除缓存 ======
        // 和 update 一样，删除后需要清两个缓存
        redisCacheService.delete(AppConstants.CACHE_KEY_CATEGORY_LIST);
        redisCacheService.delete(AppConstants.CACHE_KEY_CATEGORY_DETAIL_PREFIX + id);
    }

    private static String normalizeName(String raw) {
        return raw == null ? "" : raw.trim();
    }

    /** 是否存在「另一条」分类的展示名与 newName 相同（trim 后） */
    private boolean existsResolvedNameAmongOthers(String newName, String excludeId) {
        if (newName.isEmpty()) {
            return false;
        }
        return categoryRepository.findAll().stream()
                .anyMatch(c -> {
                    if (Objects.equals(c.getId(), excludeId)) {
                        return false;
                    }
                    String r = c.resolvedDisplayName();
                    return !r.isEmpty() && newName.equals(r);
                });
    }

    /**
     * Entity 转 VO
     *
     * <p>
     * 将数据库实体转换为视图对象，隔离内部数据结构与外部接口返回。
     * 类比前端：类似把后端返回的原始数据转换为组件需要的 props 格式。
     *
     * @param category 分类实体
     * @return 分类视图对象
     */
    private CategoryVO toVO(Category category) {
        return CategoryVO.builder()
                .id(category.getId())
                .name(category.resolvedDisplayName())
                .order(category.getOrder())
                .build();
    }
}
