package com.xyqb.platform.module.category.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xyqb.platform.common.constant.AppConstants;
import com.xyqb.platform.common.enums.BusinessCode;
import com.xyqb.platform.common.exception.BusinessException;
import com.xyqb.platform.module.category.dto.CategoryCreateDTO;
import com.xyqb.platform.module.category.service.CategoryService;
import com.xyqb.platform.common.vo.PageResult;
import com.xyqb.platform.module.category.vo.CategoryVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * CategoryController 集成测试
 *
 * <p>
 * 使用 {@code @WebMvcTest} 只启动 Spring MVC 层（Controller + 全局异常处理器 + 参数校验），
 * 不启动完整的 Spring 容器，不连接数据库，测试速度快。
 *
 * <p>
 * 测试范围：
 * <ul>
 * <li>HTTP 请求路由是否正确</li>
 * <li>请求参数校验是否生效</li>
 * <li>统一响应格式（code、businessCode、data）是否正确</li>
 * <li>异常处理是否正常工作</li>
 * </ul>
 *
 * <p>
 * 关键注解说明：
 * <ul>
 * <li>{@code @WebMvcTest(CategoryController.class)} - 只加载指定 Controller 及相关的 MVC
 * 组件</li>
 * <li>{@code @MockBean} - 将 Service 替换为 Mock 对象，注入到 Controller 中</li>
 * <li>{@code MockMvc} - Spring 提供的 HTTP 模拟工具，无需启动真实服务器即可发送请求</li>
 * </ul>
 *
 * @author XYQB Team
 * @since 1.0.0
 */
@WebMvcTest(CategoryController.class)
class CategoryControllerTest {

        /** HTTP 模拟工具，用于发送请求并验证响应 */
        @Autowired
        private MockMvc mockMvc;

        /** JSON 序列化工具，将 DTO 对象转为 JSON 字符串作为请求体 */
        @Autowired
        private ObjectMapper objectMapper;

        /** Mock 的 Service 层，控制其返回值来测试不同场景 */
        @MockBean
        private CategoryService categoryService;

        /** 接口基础路径 */
        private static final String BASE_URL = AppConstants.API_PREFIX + "/categories";

        @Test
        @DisplayName("GET /tree - 应返回分页列表和成功状态码")
        void shouldReturnCategoryTree() throws Exception {
                CategoryVO root = CategoryVO.builder()
                                .id("root-001")
                                .name("商城购物")
                                .order(10)
                                .build();

                PageResult<CategoryVO> page = PageResult.<CategoryVO>builder()
                                .list(List.of(root))
                                .total(1)
                                .pageNo(1)
                                .pageSize(20)
                                .totalPages(1)
                                .build();
                given(categoryService.getCategoryTree(anyInt(), anyInt(), nullable(String.class))).willReturn(page);

                mockMvc.perform(get(BASE_URL + "/tree"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value("0000"))
                                .andExpect(jsonPath("$.businessCode").value("0000"))
                                .andExpect(jsonPath("$.data.list[0].name").value("商城购物"))
                                .andExpect(jsonPath("$.data.list[0].order").value(10))
                                .andExpect(jsonPath("$.data.total").value(1))
                                .andExpect(jsonPath("$.data.pageNo").value(1))
                                .andExpect(jsonPath("$.data.pageSize").value(20))
                                .andExpect(jsonPath("$.data.totalPages").value(1));
        }

        @Test
        @DisplayName("GET /tree?pageNo=2&pageSize=1 - 应传入分页参数")
        void shouldPassPageParams() throws Exception {
                PageResult<CategoryVO> page = PageResult.<CategoryVO>builder()
                                .list(List.of())
                                .total(3)
                                .pageNo(2)
                                .pageSize(1)
                                .totalPages(3)
                                .build();
                given(categoryService.getCategoryTree(eq(2), eq(1), isNull())).willReturn(page);

                mockMvc.perform(get(BASE_URL + "/tree").param("pageNo", "2").param("pageSize", "1"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.pageNo").value(2))
                                .andExpect(jsonPath("$.data.pageSize").value(1))
                                .andExpect(jsonPath("$.data.total").value(3));
        }

        @Test
        @DisplayName("GET /tree?PageNo=2 - 分页参数名不区分大小写")
        void shouldAcceptPascalCasePageNo() throws Exception {
                PageResult<CategoryVO> page2 = PageResult.<CategoryVO>builder()
                                .list(List.of(CategoryVO.builder().id("p2").name("第二页").order(1).build()))
                                .total(11)
                                .pageNo(2)
                                .pageSize(10)
                                .totalPages(2)
                                .build();
                given(categoryService.getCategoryTree(eq(2), eq(10), isNull())).willReturn(page2);

                mockMvc.perform(get(BASE_URL + "/tree").param("PageNo", "2").param("pageSize", "10"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.pageNo").value(2))
                                .andExpect(jsonPath("$.data.list[0].name").value("第二页"));
        }

        @Test
        @DisplayName("GET /tree?page=2&pageSize=10 - page 作为 pageNo 别名应生效")
        void shouldAcceptPageAliasForPageNo() throws Exception {
                PageResult<CategoryVO> page2 = PageResult.<CategoryVO>builder()
                                .list(List.of(CategoryVO.builder().id("last").name("尾条").order(1).build()))
                                .total(11)
                                .pageNo(2)
                                .pageSize(10)
                                .totalPages(2)
                                .build();
                given(categoryService.getCategoryTree(eq(2), eq(10), isNull())).willReturn(page2);

                mockMvc.perform(get(BASE_URL + "/tree").param("page", "2").param("pageSize", "10"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.pageNo").value(2))
                                .andExpect(jsonPath("$.data.list[0].name").value("尾条"))
                                .andExpect(jsonPath("$.data.total").value(11));
        }

        @Test
        @DisplayName("GET /tree?pageNo=1&size=10 - size 作为 pageSize 别名应生效")
        void shouldAcceptSizeAliasForPageSize() throws Exception {
                given(categoryService.getCategoryTree(eq(1), eq(10), isNull())).willReturn(
                                PageResult.<CategoryVO>builder()
                                                .list(List.of())
                                                .total(0)
                                                .pageNo(1)
                                                .pageSize(10)
                                                .totalPages(0)
                                                .build());

                mockMvc.perform(get(BASE_URL + "/tree").param("pageNo", "1").param("size", "10"))
                                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("GET /tree?name=测试 - 应将 name 传给 Service 做模糊筛选")
        void shouldPassNameKeywordForTree() throws Exception {
                PageResult<CategoryVO> filtered = PageResult.<CategoryVO>builder()
                                .list(List.of(CategoryVO.builder().id("1").name("测试1").order(6).build()))
                                .total(5)
                                .pageNo(1)
                                .pageSize(10)
                                .totalPages(1)
                                .build();
                given(categoryService.getCategoryTree(eq(1), eq(10), eq("测试"))).willReturn(filtered);

                mockMvc.perform(get(BASE_URL + "/tree").param("pageSize", "10").param("PageNo", "1").param("name", "测试"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.total").value(5))
                                .andExpect(jsonPath("$.data.list[0].name").value("测试1"));
        }

        @Test
        @DisplayName("POST - 请求体校验失败时应返回 400 和参数校验错误码")
        void shouldValidateRequestBody() throws Exception {
                // 准备：构造空的 DTO（缺少必填字段）
                CategoryCreateDTO dto = new CategoryCreateDTO();

                // 执行 + 断言：验证参数校验生效
                mockMvc.perform(post(BASE_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.code").value("0002"));
        }

        @Test
        @DisplayName("POST - 正常创建分类应返回成功")
        void shouldCreateCategory() throws Exception {
                // 准备：构造合法的请求体和预期的返回值
                CategoryCreateDTO dto = new CategoryCreateDTO();
                dto.setCategoryName("新分类");

                CategoryVO vo = CategoryVO.builder()
                                .id("new-001")
                                .name("新分类")
                                .order(1)
                                .build();

                given(categoryService.createCategory(any(CategoryCreateDTO.class))).willReturn(vo);

                // 执行 + 断言
                mockMvc.perform(post(BASE_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value("0000"))
                                .andExpect(jsonPath("$.data.name").value("新分类"))
                                .andExpect(jsonPath("$.data.order").value(1));
        }

        @Test
        @DisplayName("GET /{id} - 分类不存在时应返回业务错误码 1001")
        void shouldHandleNotFound() throws Exception {
                // 准备：Service 抛出 CATEGORY_NOT_FOUND 异常
                given(categoryService.getCategoryById("nonexistent"))
                                .willThrow(new BusinessException(BusinessCode.CATEGORY_NOT_FOUND));

                // 执行 + 断言：验证全局异常处理器正确转换为统一响应格式
                mockMvc.perform(get(BASE_URL + "/nonexistent"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value("0002"))
                                .andExpect(jsonPath("$.businessCode").value("1001"));
        }

        @Test
        @DisplayName("POST - JSON 使用 name 字段应创建成功")
        void shouldCreateCategoryWithJsonNameField() throws Exception {
                CategoryVO vo = CategoryVO.builder().id("n1").name("手机").order(1).build();
                given(categoryService.createCategory(any(CategoryCreateDTO.class))).willReturn(vo);

                mockMvc.perform(post(BASE_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"手机\"}"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.name").value("手机"));
        }

        @Test
        @DisplayName("POST - application/x-www-form-urlencoded 使用 name 应创建成功")
        void shouldCreateCategoryWithFormName() throws Exception {
                CategoryVO vo = CategoryVO.builder().id("n2").name("表单分类").order(2).build();
                given(categoryService.createCategory(any(CategoryCreateDTO.class))).willReturn(vo);

                mockMvc.perform(post(BASE_URL)
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                .param("name", "表单分类"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.name").value("表单分类"));
        }

        @Test
        @DisplayName("PUT - JSON name 与表单 name 应更新成功")
        void shouldUpdateCategoryWithNameField() throws Exception {
                String id = "69b3e42f1b39732c60cad3e5";
                CategoryVO vo = CategoryVO.builder().id(id).name("手机").order(5).build();
                given(categoryService.updateCategory(any(), any())).willReturn(vo);

                mockMvc.perform(put(BASE_URL + "/" + id)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"手机\"}"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.name").value("手机"));

                mockMvc.perform(put(BASE_URL + "/" + id)
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                .param("name", "手机"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.name").value("手机"));
        }
}
