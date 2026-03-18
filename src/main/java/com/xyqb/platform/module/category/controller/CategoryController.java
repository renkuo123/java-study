package com.xyqb.platform.module.category.controller;

import com.xyqb.platform.common.annotation.RateLimit;
import com.xyqb.platform.common.constant.AppConstants;
import com.xyqb.platform.common.response.ApiResponse;
import com.xyqb.platform.common.vo.PageResult;
import com.xyqb.platform.module.category.dto.CategoryCreateDTO;
import com.xyqb.platform.module.category.dto.CategoryUpdateDTO;
import com.xyqb.platform.module.category.service.CategoryService;
import com.xyqb.platform.module.category.vo.CategoryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 分类管理 Controller
 *
 * <p>提供分类的 RESTful CRUD 接口，遵循以下规范：
 * <ul>
 *     <li>GET    /api/v1/categories/tree         → 获取列表（按 order 排序，分页参数 pageNo、pageSize）</li>
 *     <li>GET    /api/v1/categories/{id}          → 获取单个分类</li>
 *     <li>POST   /api/v1/categories               → 创建分类（JSON 或 {@code application/x-www-form-urlencoded}，名称字段 {@code name}）</li>
 *     <li>PUT    /api/v1/categories/{id}          → 更新分类（同上）</li>
 *     <li>DELETE /api/v1/categories/{id}          → 删除分类</li>
 * </ul>
 *
 * <p>设计说明：
 * <ul>
 *     <li>Controller 只负责接收请求、参数校验、调用 Service、包装响应，不包含业务逻辑</li>
 *     <li>所有方法返回 {@link ApiResponse} 统一格式</li>
 *     <li>{@code @Valid} 触发 DTO 上的参数校验，校验失败自动抛出异常并被全局处理</li>
 *     <li>{@code @RateLimit} 对每个接口独立限流，读接口 20 QPS，写接口 5 QPS</li>
 * </ul>
 *
 * @author XYQB Team
 * @since 1.0.0
 */
@Tag(name = "分类管理", description = "分类的增删改查及树形结构接口")
@RestController
@RequestMapping(AppConstants.API_PREFIX + "/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    /**
     * 获取分类列表（分页）
     *
     * <p>查询参数：{@code pageNo}（默认 1）、{@code pageSize}（默认 20，最大 100）。
     * 响应 {@code data} 为 {@link PageResult}：{@code list}、{@code total}、{@code pageNo}、{@code pageSize}、{@code totalPages}。
     */
    @Operation(summary = "获取分类列表", description = "分页参数名**不区分大小写**。可选 **name**：对分类展示名子串模糊匹配（英文忽略大小写），再分页。pageNo 优先于 page；pageSize 优先于 size。")
    @GetMapping("/tree")
    @RateLimit(permitsPerSecond = 20)
    public ApiResponse<PageResult<CategoryVO>> getCategoryTree(HttpServletRequest request) {
        int pn = resolvePageNoFromRequest(request);
        int ps = resolvePageSizeFromRequest(request);
        String name = findStringParameterIgnoreCaseKey(request, "name");
        return ApiResponse.success(categoryService.getCategoryTree(pn, ps, name));
    }

    /** 先匹配 pageNo（任意大小写），再无则 page */
    private static int resolvePageNoFromRequest(HttpServletRequest request) {
        Integer v = findIntParameterIgnoreCaseKey(request, "pageNo");
        if (v != null) {
            return v;
        }
        v = findIntParameterIgnoreCaseKey(request, "page");
        return v != null ? v : 1;
    }

    /** 先匹配 pageSize，再无则 size */
    private static int resolvePageSizeFromRequest(HttpServletRequest request) {
        Integer v = findIntParameterIgnoreCaseKey(request, "pageSize");
        if (v != null) {
            return v;
        }
        v = findIntParameterIgnoreCaseKey(request, "size");
        return v != null ? v : 20;
    }

    /** 查询串中与 canonical 名称忽略大小写相等的第一个参数，解析为 int */
    private static Integer findIntParameterIgnoreCaseKey(HttpServletRequest request, String canonicalName) {
        for (Map.Entry<String, String[]> e : request.getParameterMap().entrySet()) {
            if (canonicalName.equalsIgnoreCase(e.getKey())) {
                String[] values = e.getValue();
                if (values != null && values.length > 0 && !values[0].isBlank()) {
                    try {
                        return Integer.parseInt(values[0].trim());
                    } catch (NumberFormatException ignored) {
                        return null;
                    }
                }
            }
        }
        return null;
    }

    /** 与 canonical 键名忽略大小写相等的第一个非空参数值（去首尾空白）；无则 {@code null} */
    private static String findStringParameterIgnoreCaseKey(HttpServletRequest request, String canonicalName) {
        for (Map.Entry<String, String[]> e : request.getParameterMap().entrySet()) {
            if (canonicalName.equalsIgnoreCase(e.getKey())) {
                String[] values = e.getValue();
                if (values != null && values.length > 0) {
                    String v = values[0] == null ? "" : values[0].trim();
                    if (!v.isEmpty()) {
                        return v;
                    }
                }
            }
        }
        return null;
    }

    /**
     * 根据 ID 获取分类详情
     *
     * @param id 分类 ID（MongoDB ObjectId）
     * @return 分类详情
     */
    @Operation(summary = "根据ID获取分类详情")
    @GetMapping("/{id}")
    @RateLimit(permitsPerSecond = 20)
    public ApiResponse<CategoryVO> getCategoryById(
            @Parameter(description = "分类ID") @PathVariable String id) {
        return ApiResponse.success(categoryService.getCategoryById(id));
    }

    /**
     * 创建分类
     *
     * <p>{@code @Valid} 会自动触发 {@link CategoryCreateDTO} 上的校验注解，
     * 校验失败时抛出 {@code MethodArgumentNotValidException}，由全局异常处理器返回 400。
     *
     * @param dto 创建请求体
     * @return 创建成功的分类信息
     */
    @Operation(summary = "创建分类", description = "请求体：JSON 或表单；分类名称字段支持 name（与 categoryName 等价）。可选请求头：Access-Token、X-Auth-Token、qg-tenant-id。")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @RateLimit(permitsPerSecond = 5)
    public ApiResponse<CategoryVO> createCategory(@Valid @RequestBody CategoryCreateDTO dto) {
        return ApiResponse.success(categoryService.createCategory(dto));
    }

    @Operation(summary = "创建分类（表单）", description = "Content-Type: application/x-www-form-urlencoded，字段 name=分类名称")
    @PostMapping(consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    @RateLimit(permitsPerSecond = 5)
    public ApiResponse<CategoryVO> createCategoryForm(@Valid @ModelAttribute CategoryCreateDTO dto) {
        return ApiResponse.success(categoryService.createCategory(dto));
    }

    /**
     * 更新分类
     *
     * @param id  分类 ID
     * @param dto 更新请求体
     * @return 更新后的分类信息
     */
    @Operation(summary = "更新分类", description = "JSON 或表单；名称字段支持 name（与 categoryName 等价）。可选请求头同创建接口。")
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @RateLimit(permitsPerSecond = 5)
    public ApiResponse<CategoryVO> updateCategory(
            @Parameter(description = "分类ID") @PathVariable String id,
            @Valid @RequestBody CategoryUpdateDTO dto) {
        return ApiResponse.success(categoryService.updateCategory(id, dto));
    }

    @Operation(summary = "更新分类（表单）", description = "Content-Type: application/x-www-form-urlencoded，字段 name=新名称")
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    @RateLimit(permitsPerSecond = 5)
    public ApiResponse<CategoryVO> updateCategoryForm(
            @Parameter(description = "分类ID") @PathVariable String id,
            @Valid @ModelAttribute CategoryUpdateDTO dto) {
        return ApiResponse.success(categoryService.updateCategory(id, dto));
    }

    /**
     * 删除分类
     *
     * @param id 分类 ID
     * @return 空数据的成功响应
     */
    @Operation(summary = "删除分类")
    @DeleteMapping("/{id}")
    @RateLimit(permitsPerSecond = 5)
    public ApiResponse<Void> deleteCategory(
            @Parameter(description = "分类ID") @PathVariable String id) {
        categoryService.deleteCategory(id);
        return ApiResponse.success();
    }
}
