package com.xyqb.platform.module.category.service;

import com.xyqb.platform.common.vo.PageResult;
import com.xyqb.platform.module.category.dto.CategoryCreateDTO;
import com.xyqb.platform.module.category.dto.CategoryUpdateDTO;
import com.xyqb.platform.module.category.vo.CategoryVO;

/**
 * 分类管理服务接口
 *
 * <p>定义分类模块的所有业务操作契约，遵循面向接口编程原则。
 * 实现类为 {@link com.xyqb.platform.module.category.service.impl.CategoryServiceImpl}。
 *
 * <p>面向接口编程的好处：
 * <ul>
 *     <li>Controller 只依赖接口，不依赖具体实现，满足依赖倒置原则（SOLID 中的 D）</li>
 *     <li>便于单元测试时 Mock 替换</li>
 *     <li>未来需要切换实现时（如从 MongoDB 迁移到其他存储），只需新增实现类</li>
 * </ul>
 *
 * @author XYQB Team
 * @since 1.0.0
 */
public interface CategoryService {

    /**
     * 获取分类列表（分页）
     *
     * <p>当前对应 MongoDB 的 {@code suggestType} 集合，业务上为扁平列表，
     * 按 {@code order} 字段倒序后分页返回。
     *
     * @param pageNo       页码，从 1 开始；非法值按 1 处理
     * @param pageSize     每页条数；小于 1 按默认 20，大于上限按 100
     * @param nameKeyword  可选；非空时对展示名做子串模糊匹配（英文忽略大小写），再排序分页
     * @return 分页结果（含 list、total、pageNo、pageSize、totalPages）
     */
    PageResult<CategoryVO> getCategoryTree(int pageNo, int pageSize, String nameKeyword);

    /**
     * 同 {@link #getCategoryTree(int, int, String)}，不按名称筛选。
     */
    default PageResult<CategoryVO> getCategoryTree(int pageNo, int pageSize) {
        return getCategoryTree(pageNo, pageSize, null);
    }

    /**
     * 根据 ID 获取单个分类详情
     *
     * @param id 分类 ID
     * @return 分类详情 VO
     * @throws com.xyqb.platform.common.exception.BusinessException 分类不存在时抛出 CATEGORY_NOT_FOUND
     */
    CategoryVO getCategoryById(String id);

    /**
     * 创建新分类
     *
     * <p>创建前会进行以下校验：
     * <ul>
     *     <li>名称全局唯一</li>
     * </ul>
     *
     * @param dto 创建请求参数
     * @return 创建成功的分类 VO（包含自动生成的 ID）
     * @throws com.xyqb.platform.common.exception.BusinessException 校验不通过时抛出对应异常
     */
    CategoryVO createCategory(CategoryCreateDTO dto);

    /**
     * 更新分类信息
     *
     * <p>当前只支持修改分类名称，修改前会校验名称唯一性。
     *
     * @param id  分类 ID
     * @param dto 更新请求参数
     * @return 更新后的分类 VO
     * @throws com.xyqb.platform.common.exception.BusinessException 分类不存在或名称重复时抛出
     */
    CategoryVO updateCategory(String id, CategoryUpdateDTO dto);

    /**
     * 删除分类
     *
     * @param id 分类 ID
     * @throws com.xyqb.platform.common.exception.BusinessException 分类不存在时抛出
     */
    void deleteCategory(String id);
}
