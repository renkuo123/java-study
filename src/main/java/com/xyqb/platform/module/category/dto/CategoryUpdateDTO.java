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
 * 分类更新请求 DTO
 *
 * <p>表单若同时传空 {@code categoryName} 与有效 {@code name}，空白 {@code categoryName} 不覆盖已有名称。
 */
@Getter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
public class CategoryUpdateDTO {

    @JsonAlias({"name"})
    @NotBlank(message = "分类名称不能为空")
    @Size(max = 50, message = "分类名称最长50个字符")
    private String categoryName;

    public void setCategoryName(String v) {
        if (v != null && !v.isBlank()) {
            this.categoryName = v.trim();
        }
    }

    @JsonIgnore
    public String getName() {
        return categoryName;
    }

    public void setName(String name) {
        if (name == null || name.isBlank()) {
            return;
        }
        if (this.categoryName == null || this.categoryName.isBlank()) {
            this.categoryName = name.trim();
        }
    }
}
