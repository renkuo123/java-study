package com.xyqb.platform.module.category.service;

import com.xyqb.platform.common.constant.AppConstants;
import com.xyqb.platform.common.enums.BusinessCode;
import com.xyqb.platform.common.exception.BusinessException;
import com.xyqb.platform.common.service.RedisCacheService;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * CategoryService 单元测试
 *
 * <p>
 * 使用 Mockito 框架 Mock 掉 Repository 层和 RedisCacheService，隔离测试 Service 层的业务逻辑。
 *
 * <p>
 * 测试规范：
 * <ul>
 * <li>{@code @ExtendWith(MockitoExtension.class)} - 启用 Mockito 注解支持，无需启动 Spring
 * 容器</li>
 * <li>{@code @Mock} - 创建 Repository 和 RedisCacheService 的模拟对象</li>
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

    /**
     * Mock 的 Redis 缓存服务，不会真正访问 Redis
     *
     * <p>
     * 新增：因为 CategoryServiceImpl 现在依赖 RedisCacheService，
     * 所以测试中也要 mock 它。@InjectMocks 会自动把这个 mock 注入到 categoryService 中。
     * Mockito 的 mock 对象默认行为：方法返回 null / 0 / false（根据返回类型），
     * 这正好模拟了"缓存未命中"的场景。
     * </p>
     */
    @Mock
    private RedisCacheService redisCacheService;

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
        @DisplayName("数据库为空时应返回空分页，并以短 TTL 缓存空结果防止穿透")
        void shouldReturnEmptyListWhenNoCategoriesExist() {
            // given — 缓存未命中（返回 null），DB 也为空
            given(redisCacheService.getList(eq(AppConstants.CACHE_KEY_CATEGORY_LIST), eq(Category.class)))
                    .willReturn(null);
            given(categoryRepository.findAll()).willReturn(Collections.emptyList());

            PageResult<CategoryVO> page = categoryService.getCategoryTree(1, 20);

            assertThat(page.getList()).isEmpty();
            assertThat(page.getTotal()).isZero();
            assertThat(page.getPageNo()).isEqualTo(1);
            assertThat(page.getPageSize()).isEqualTo(20);
            assertThat(page.getTotalPages()).isZero();

            // 验证空结果也被缓存了（使用短 TTL 防止缓存穿透）
            verify(redisCacheService).set(
                    eq(AppConstants.CACHE_KEY_CATEGORY_LIST),
                    eq(Collections.emptyList()),
                    eq(AppConstants.CACHE_EMPTY_TTL_MINUTES),
                    any()
            );
        }

        @Test
        @DisplayName("缓存未命中时应查询 MongoDB 并写入缓存")
        void shouldQueryDbAndSetCacheWhenCacheMiss() {
            // given — 缓存未命中
            given(redisCacheService.getList(eq(AppConstants.CACHE_KEY_CATEGORY_LIST), eq(Category.class)))
                    .willReturn(null);
            given(categoryRepository.findAll()).willReturn(List.of(rootCategory, childCategory));

            // when
            PageResult<CategoryVO> page = categoryService.getCategoryTree(1, 20);

            // then — 验证查了 DB 且把结果写入了缓存
            verify(categoryRepository).findAll();
            verify(redisCacheService).set(
                    eq(AppConstants.CACHE_KEY_CATEGORY_LIST),
                    any(List.class),
                    eq(AppConstants.CACHE_CATEGORY_TTL_MINUTES),
                    any()
            );
            assertThat(page.getList()).hasSize(2);
        }

        @Test
        @DisplayName("缓存命中时不应查询 MongoDB")
        void shouldUseCachedDataWhenCacheHit() {
            // given — 缓存命中，直接返回数据
            given(redisCacheService.getList(eq(AppConstants.CACHE_KEY_CATEGORY_LIST), eq(Category.class)))
                    .willReturn(List.of(rootCategory, childCategory));

            // when
            PageResult<CategoryVO> page = categoryService.getCategoryTree(1, 20);

            // then — 不应该调用 DB
            verify(categoryRepository, never()).findAll();
            assertThat(page.getList()).hasSize(2);
            assertThat(page.getList().get(0).getName()).isEqualTo("商城购物");
        }

        @Test
        @DisplayName("第一页应按 order 倒序返回扁平列表")
        void shouldReturnFlatListSortedByOrderDesc() {
            given(redisCacheService.getList(eq(AppConstants.CACHE_KEY_CATEGORY_LIST), eq(Category.class)))
                    .willReturn(null);
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
            given(redisCacheService.getList(eq(AppConstants.CACHE_KEY_CATEGORY_LIST), eq(Category.class)))
                    .willReturn(null);
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
            given(redisCacheService.getList(eq(AppConstants.CACHE_KEY_CATEGORY_LIST), eq(Category.class)))
                    .willReturn(null);
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
            given(redisCacheService.getList(eq(AppConstants.CACHE_KEY_CATEGORY_LIST), eq(Category.class)))
                    .willReturn(null);
            given(categoryRepository.findAll()).willReturn(List.of(cOther, rootCategory, cTest, childCategory));

            PageResult<CategoryVO> page = categoryService.getCategoryTree(1, 10, "测试");

            assertThat(page.getTotal()).isEqualTo(1);
            assertThat(page.getList()).hasSize(1);
            assertThat(page.getList().get(0).getName()).isEqualTo("测试1");
        }

        @Test
        @DisplayName("name 无匹配时 total 为 0")
        void shouldReturnEmptyWhenNameMatchesNothing() {
            given(redisCacheService.getList(eq(AppConstants.CACHE_KEY_CATEGORY_LIST), eq(Category.class)))
                    .willReturn(null);
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
        @DisplayName("缓存命中时应直接返回，不查 MongoDB")
        void shouldReturnCachedCategoryWhenCacheHit() {
            // given — 缓存中有这条数据
            String cacheKey = AppConstants.CACHE_KEY_CATEGORY_DETAIL_PREFIX + "root-001";
            given(redisCacheService.get(eq(cacheKey), eq(Category.class)))
                    .willReturn(rootCategory);

            // when
            CategoryVO vo = categoryService.getCategoryById("root-001");

            // then — 不应查 DB
            verify(categoryRepository, never()).findById(anyString());
            assertThat(vo.getId()).isEqualTo("root-001");
            assertThat(vo.getName()).isEqualTo("商城购物");
        }

        @Test
        @DisplayName("缓存未命中时应查 MongoDB 并写入缓存")
        void shouldQueryDbAndSetCacheWhenCacheMiss() {
            // given — 缓存未命中
            String cacheKey = AppConstants.CACHE_KEY_CATEGORY_DETAIL_PREFIX + "root-001";
            given(redisCacheService.get(eq(cacheKey), eq(Category.class)))
                    .willReturn(null);
            given(categoryRepository.findById("root-001")).willReturn(Optional.of(rootCategory));

            // when
            CategoryVO vo = categoryService.getCategoryById("root-001");

            // then — 查了 DB 且写入了缓存
            verify(categoryRepository).findById("root-001");
            verify(redisCacheService).set(eq(cacheKey), eq(rootCategory), anyLong(), any());
            assertThat(vo.getId()).isEqualTo("root-001");
        }

        @Test
        @DisplayName("分类不存在时应抛出 BusinessException")
        void shouldThrowWhenNotFound() {
            String cacheKey = AppConstants.CACHE_KEY_CATEGORY_DETAIL_PREFIX + "nonexistent";
            given(redisCacheService.get(eq(cacheKey), eq(Category.class)))
                    .willReturn(null);
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
        @DisplayName("正常创建根分类，并清除列表缓存")
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

            // 验证创建后清除了列表缓存
            verify(redisCacheService).delete(AppConstants.CACHE_KEY_CATEGORY_LIST);
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
        @DisplayName("名称未改（与当前展示名一致）应成功，并清除缓存")
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

            // 验证更新后清除了列表缓存和详情缓存
            verify(redisCacheService).delete(AppConstants.CACHE_KEY_CATEGORY_LIST);
            verify(redisCacheService).delete(AppConstants.CACHE_KEY_CATEGORY_DETAIL_PREFIX + "a1");
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
        @DisplayName("正常删除，并清除列表缓存和详情缓存")
        void shouldDeleteAndInvalidateCaches() {
            given(categoryRepository.existsById("child-001")).willReturn(true);

            categoryService.deleteCategory("child-001");

            verify(categoryRepository).deleteById("child-001");

            // 验证删除后清除了两个缓存
            verify(redisCacheService).delete(AppConstants.CACHE_KEY_CATEGORY_LIST);
            verify(redisCacheService).delete(AppConstants.CACHE_KEY_CATEGORY_DETAIL_PREFIX + "child-001");
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
