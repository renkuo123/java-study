package com.xyqb.platform.module.category.service.impl;

import com.xyqb.platform.common.enums.BusinessCode;
import com.xyqb.platform.common.exception.BusinessException;
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
 * <li>通过构造器注入 Repository（{@code @RequiredArgsConstructor}），满足依赖注入最佳实践</li>
 * <li>所有数据库实体（Entity）在 Service 层转换为视图对象（VO）后返回，Controller 不直接接触 Entity</li>
 * <li>业务校验失败统一抛出 {@link BusinessException}，由全局异常处理器转换为标准响应</li>
 * </ul>
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
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final CategoryRepository categoryRepository;

    /**
     * {@inheritDoc}
     *
     * <p>
     * 实现策略：一次查询全部数据 + 内存组装树，时间复杂度 O(n)，空间复杂度 O(n)。
     * 适用于分类总量在万级以下的场景。如果数据量增长到十万级以上，
     * 需要考虑改为按需加载（懒加载子节点）或缓存方案。
     */
    @Override
    public PageResult<CategoryVO> getCategoryTree(int pageNo, int pageSize, String nameKeyword) {
        int pn = pageNo < 1 ? 1 : pageNo;
        int ps = pageSize < 1 ? DEFAULT_PAGE_SIZE : Math.min(pageSize, MAX_PAGE_SIZE);

        List<Category> allCategories = categoryRepository.findAll();
        if (allCategories.isEmpty()) {
            return PageResult.empty(pn, ps);
        }

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

    /** {@inheritDoc} */
    @Override
    public CategoryVO getCategoryById(String id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new BusinessException(BusinessCode.CATEGORY_NOT_FOUND));
        return toVO(category);
    }

    /** {@inheritDoc} */
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
        return toVO(saved);
    }

    /** {@inheritDoc} */
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
        return toVO(updated);
    }

    /** {@inheritDoc} */
    @Override
    public void deleteCategory(String id) {
        if (!categoryRepository.existsById(id)) {
            throw new BusinessException(BusinessCode.CATEGORY_NOT_FOUND);
        }

        categoryRepository.deleteById(id);
        log.info("分类已删除: id={}", id);
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
