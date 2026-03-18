package com.xyqb.platform.module.category.service;

import com.xyqb.platform.common.enums.BusinessCode;
import com.xyqb.platform.common.exception.BusinessException;
import com.xyqb.platform.module.category.dto.CategoryCreateDTO;
import com.xyqb.platform.module.category.dto.CategoryUpdateDTO;
import com.xyqb.platform.module.category.entity.Category;
import com.xyqb.platform.module.category.repository.CategoryRepository;
import com.xyqb.platform.module.category.service.impl.CategoryServiceImpl;
import com.xyqb.platform.common.vo.PageResult;
import com.xyqb.platform.module.category.vo.CategoryVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * CategoryService 单元测试
 *
 * <p>
 * 使用 Mockito 框架 Mock 掉 Repository 层，隔离测试 Service 层的业务逻辑。
 *
 * <p>
 * 测试规范：
 * <ul>
 * <li>{@code @ExtendWith(MockitoExtension.class)} - 启用 Mockito 注解支持，无需启动 Spring
 * 容器</li>
 * <li>{@code @Mock} - 创建 Repository 的模拟对象</li>
 * <li>{@code @InjectMocks} - 自动将 Mock 对象注入到 Service 实现类中</li>
 * <li>{@code @Nested} - 按方法分组组织测试用例，结构清晰</li>
 * <li>使用 BDD 风格：given（准备）→ when（执行）→ then（断言）</li>
 * </ul>
 *
 * @author XYQB Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    /** Mock 的分类 Repository，不会真正访问数据库 */
    @Mock
    private CategoryRepository categoryRepository;

    /** 被测试的 Service 实现类，自动注入上面的 Mock 对象 */
    @InjectMocks
    private CategoryServiceImpl categoryService;

    /** 测试用的根分类数据 */
    private Category rootCategory;

    /** 测试用的子分类数据 */
    private Category childCategory;

    /**
     * 每个测试方法执行前初始化测试数据
     *
     * <p>
     * {@code @BeforeEach} 确保每个测试用例使用全新的数据，互不影响。
     */
    @BeforeEach
    void setUp() {
        rootCategory = new Category();
        rootCategory.setId("root-001");
        rootCategory.setCategoryName("商城购物");
        rootCategory.setOrder(10);

        childCategory = new Category();
        childCategory.setId("child-001");
        childCategory.setCategoryName("售前-咨询");
        childCategory.setOrder(3);
    }

    /** 获取分类树 相关测试 */
    @Nested
    @DisplayName("getCategoryTree - 获取分类树形结构")
    class GetCategoryTreeTests {

        @Test
        @DisplayName("数据库为空时应返回空分页")
        void shouldReturnEmptyListWhenNoCategoriesExist() {
            given(categoryRepository.findAll()).willReturn(Collections.emptyList());

            PageResult<CategoryVO> page = categoryService.getCategoryTree(1, 20);

            assertThat(page.getList()).isEmpty();
            assertThat(page.getTotal()).isZero();
            assertThat(page.getPageNo()).isEqualTo(1);
            assertThat(page.getPageSize()).isEqualTo(20);
            assertThat(page.getTotalPages()).isZero();
        }

        @Test
        @DisplayName("第一页应按 order 倒序返回扁平列表")
        void shouldReturnFlatListSortedByOrderDesc() {
            given(categoryRepository.findAll()).willReturn(List.of(rootCategory, childCategory));

            PageResult<CategoryVO> page = categoryService.getCategoryTree(1, 20);

            assertThat(page.getList()).hasSize(2);
            assertThat(page.getTotal()).isEqualTo(2);
            assertThat(page.getList().get(0).getName()).isEqualTo("商城购物");
            assertThat(page.getList().get(0).getOrder()).isEqualTo(10);
            assertThat(page.getList().get(1).getName()).isEqualTo("售前-咨询");
            assertThat(page.getList().get(1).getOrder()).isEqualTo(3);
            assertThat(page.getTotalPages()).isEqualTo(1);
        }

        @Test
        @DisplayName("pageSize=1 第二页应只含第二条")
        void shouldReturnSecondPageWhenPageNoIs2() {
            given(categoryRepository.findAll()).willReturn(List.of(rootCategory, childCategory));

            PageResult<CategoryVO> page = categoryService.getCategoryTree(2, 1);

            assertThat(page.getList()).hasSize(1);
            assertThat(page.getList().get(0).getName()).isEqualTo("售前-咨询");
            assertThat(page.getTotal()).isEqualTo(2);
            assertThat(page.getTotalPages()).isEqualTo(2);
        }

        @Test
        @DisplayName("pageNo 非法时按 1 处理；pageSize 超过上限按 100")
        void shouldNormalizePageParams() {
            given(categoryRepository.findAll()).willReturn(List.of(rootCategory));

            PageResult<CategoryVO> p1 = categoryService.getCategoryTree(0, 20);
            assertThat(p1.getPageNo()).isEqualTo(1);

            PageResult<CategoryVO> p2 = categoryService.getCategoryTree(1, 200);
            assertThat(p2.getPageSize()).isEqualTo(100);
        }

        @Test
        @DisplayName("name 关键词：仅保留展示名包含子串的分类，再按 order 倒序分页")
        void shouldFilterByNameKeyword() {
            Category cTest = new Category();
            cTest.setId("t1");
            cTest.setCategoryName("测试1");
            cTest.setOrder(6);
            Category cOther = new Category();
            cOther.setId("o1");
            cOther.setCategoryName("付款问题");
            cOther.setOrder(1);
            given(categoryRepository.findAll()).willReturn(List.of(cOther, rootCategory, cTest, childCategory));

            PageResult<CategoryVO> page = categoryService.getCategoryTree(1, 10, "测试");

            assertThat(page.getTotal()).isEqualTo(1);
            assertThat(page.getList()).hasSize(1);
            assertThat(page.getList().get(0).getName()).isEqualTo("测试1");
        }

        @Test
        @DisplayName("name 无匹配时 total 为 0")
        void shouldReturnEmptyWhenNameMatchesNothing() {
            given(categoryRepository.findAll()).willReturn(List.of(rootCategory));

            PageResult<CategoryVO> page = categoryService.getCategoryTree(1, 10, "不存在的关键词");

            assertThat(page.getTotal()).isZero();
            assertThat(page.getList()).isEmpty();
        }
    }

    /** 根据 ID 查询 相关测试 */
    @Nested
    @DisplayName("getCategoryById - 根据ID查询分类")
    class GetCategoryByIdTests {

        @Test
        @DisplayName("分类存在时应正常返回")
        void shouldReturnCategoryWhenFound() {
            given(categoryRepository.findById("root-001")).willReturn(Optional.of(rootCategory));

            CategoryVO vo = categoryService.getCategoryById("root-001");

            assertThat(vo.getId()).isEqualTo("root-001");
            assertThat(vo.getName()).isEqualTo("商城购物");
        }

        @Test
        @DisplayName("分类不存在时应抛出 BusinessException")
        void shouldThrowWhenNotFound() {
            given(categoryRepository.findById("nonexistent")).willReturn(Optional.empty());

            assertThatThrownBy(() -> categoryService.getCategoryById("nonexistent"))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getBusinessCode())
                    .isEqualTo(BusinessCode.CATEGORY_NOT_FOUND);
        }
    }

    /** 创建分类 相关测试 */
    @Nested
    @DisplayName("createCategory - 创建分类")
    class CreateCategoryTests {

        @Test
        @DisplayName("正常创建根分类")
        void shouldCreateRootCategory() {
            CategoryCreateDTO dto = new CategoryCreateDTO();
            dto.setCategoryName("新分类");

            // Service 会先 findAll() 用于去重+计算最大 order
            Category existing1 = new Category();
            existing1.setId("old-001");
            existing1.setCategoryName("旧分类A");
            existing1.setOrder(3);
            Category existing2 = new Category();
            existing2.setId("old-002");
            existing2.setCategoryName("旧分类B");
            existing2.setOrder(8);
            given(categoryRepository.findAll()).willReturn(List.of(existing1, existing2));

            given(categoryRepository.save(any(Category.class))).willAnswer(invocation -> {
                Category saved = invocation.getArgument(0);
                saved.setId("new-001");
                return saved;
            });

            CategoryVO result = categoryService.createCategory(dto);

            assertThat(result.getName()).isEqualTo("新分类");
            assertThat(result.getOrder()).isEqualTo(9);
            verify(categoryRepository).save(any(Category.class));
        }

        @Test
        @DisplayName("同一父分类下名称重复时应抛出异常")
        void shouldThrowWhenNameDuplicate() {
            CategoryCreateDTO dto = new CategoryCreateDTO();
            dto.setCategoryName("商城购物");

            Category existing = new Category();
            existing.setId("old-001");
            existing.setCategoryName("商城购物");
            existing.setOrder(1);
            given(categoryRepository.findAll()).willReturn(List.of(existing));

            assertThatThrownBy(() -> categoryService.createCategory(dto))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getBusinessCode())
                    .isEqualTo(BusinessCode.CATEGORY_NAME_DUPLICATE);

            // 验证名称重复时不会调用 save
            verify(categoryRepository, never()).save(any());
        }

        @Test
        @DisplayName("与仅存在 legacy 字段 categoryName 的文档重名时应拒绝创建")
        void shouldThrowWhenDuplicateAgainstLegacyFieldOnly() {
            CategoryCreateDTO dto = new CategoryCreateDTO();
            dto.setCategoryName("手机");

            Category legacy = new Category();
            legacy.setId("old-002");
            legacy.setCategoryName(null);
            legacy.setLegacyCategoryName("手机");
            legacy.setOrder(1);
            given(categoryRepository.findAll()).willReturn(List.of(legacy));

            assertThatThrownBy(() -> categoryService.createCategory(dto))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getBusinessCode())
                    .isEqualTo(BusinessCode.CATEGORY_NAME_DUPLICATE);
            verify(categoryRepository, never()).save(any());
        }

        @Test
        @DisplayName("库中存在无名称历史文档时不应误判为名称重复")
        void shouldNotTreatBlankDbNamesAsDuplicate() {
            CategoryCreateDTO dto = new CategoryCreateDTO();
            dto.setCategoryName("1其他");

            Category blank = new Category();
            blank.setId("legacy-empty");
            blank.setCategoryName(null);
            blank.setLegacyCategoryName(null);
            blank.setOrder(0);
            given(categoryRepository.findAll()).willReturn(List.of(blank));
            given(categoryRepository.save(any(Category.class))).willAnswer(invocation -> {
                Category saved = invocation.getArgument(0);
                saved.setId("new-id");
                return saved;
            });

            CategoryVO result = categoryService.createCategory(dto);

            assertThat(result.getName()).isEqualTo("1其他");
            verify(categoryRepository).save(any(Category.class));
        }
    }

    @Nested
    @DisplayName("updateCategory - 更新分类")
    class UpdateCategoryTests {

        @Test
        @DisplayName("名称未改（与当前展示名一致）应成功")
        void shouldPassWhenNameUnchanged() {
            Category self = new Category();
            self.setId("a1");
            self.setCategoryName("手机");
            self.setOrder(1);

            given(categoryRepository.findById("a1")).willReturn(Optional.of(self));
            given(categoryRepository.save(any(Category.class))).willAnswer(i -> i.getArgument(0));

            CategoryUpdateDTO dto = new CategoryUpdateDTO();
            dto.setCategoryName("手机");

            CategoryVO vo = categoryService.updateCategory("a1", dto);

            assertThat(vo.getName()).isEqualTo("手机");
            verify(categoryRepository).save(any(Category.class));
        }

        @Test
        @DisplayName("改成与其它分类已有名称相同应拒绝")
        void shouldThrowWhenRenamingToExistingName() {
            Category self = new Category();
            self.setId("a1");
            self.setCategoryName("手机");
            self.setOrder(1);
            Category other = new Category();
            other.setId("b1");
            other.setLegacyCategoryName("电脑");
            other.setOrder(2);

            given(categoryRepository.findById("a1")).willReturn(Optional.of(self));
            given(categoryRepository.findAll()).willReturn(List.of(self, other));

            CategoryUpdateDTO dto = new CategoryUpdateDTO();
            dto.setCategoryName("电脑");

            assertThatThrownBy(() -> categoryService.updateCategory("a1", dto))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getBusinessCode())
                    .isEqualTo(BusinessCode.CATEGORY_NAME_DUPLICATE);
            verify(categoryRepository, never()).save(any());
        }
    }

    /** 删除分类 相关测试 */
    @Nested
    @DisplayName("deleteCategory - 删除分类")
    class DeleteCategoryTests {

        @Test
        @DisplayName("无子分类时应正常删除")
        void shouldDeleteWhenNoChildren() {
            given(categoryRepository.existsById("child-001")).willReturn(true);

            categoryService.deleteCategory("child-001");

            verify(categoryRepository).deleteById("child-001");
        }

        @Test
        @DisplayName("分类不存在时应抛出异常")
        void shouldThrowWhenNotExists() {
            given(categoryRepository.existsById("nonexistent")).willReturn(false);

            assertThatThrownBy(() -> categoryService.deleteCategory("nonexistent"))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getBusinessCode())
                    .isEqualTo(BusinessCode.CATEGORY_NOT_FOUND);
        }
    }
}
