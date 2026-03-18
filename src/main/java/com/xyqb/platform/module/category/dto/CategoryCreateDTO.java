package com.xyqb.platform.module.category.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * 分类创建请求 DTO（Data Transfer Object）
 *
 * <p>用于接收客户端创建分类的请求参数，与 Entity 层解耦。
 * 通过 Jakarta Validation 注解实现参数自动校验，
 * 校验失败时由 {@link com.xyqb.platform.common.exception.GlobalExceptionHandler}
 * 统一处理。
 *
 * <p>请求体示例：
 * <pre>{@code
 * {
 * "name": "售前-咨询"
 * }
 * 或与 {@code categoryName} 等价
 * }</pre>
 *
 * <p>表单若同时传空 {@code categoryName} 与有效 {@code name}，忽略空白 {@code categoryName}，
 * 避免绑定顺序导致名称被清空。
 */
@Getter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
public class CategoryCreateDTO {

    @JsonAlias({"name"})
    @NotBlank(message = "分类名称不能为空")
    @Size(max = 50, message = "分类名称最长50个字符")
    private String categoryName;

    public void setCategoryName(String v) {
        if (v != null && !v.isBlank()) {
            this.categoryName = v.trim();
        }
    }

    /** 与 {@link #setName} 成对，保证表单 {@code name=} 能被 Spring 绑定 */
    @JsonIgnore
    public String getName() {
        return categoryName;
    }

    /** 表单字段 {@code name} */
    public void setName(String name) {
        if (name == null || name.isBlank()) {
            return;
        }
        if (this.categoryName == null || this.categoryName.isBlank()) {
            this.categoryName = name.trim();
        }
    }
}
