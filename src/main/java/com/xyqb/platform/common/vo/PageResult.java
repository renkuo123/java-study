package com.xyqb.platform.common.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

/**
 * 通用分页结果
 *
 * @param <T> 列表元素类型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PageResult<T> {

    /** 当前页数据 */
    private List<T> list;

    /** 总条数 */
    private long total;

    /** 当前页码，从 1 开始 */
    private int pageNo;

    /** 每页条数 */
    private int pageSize;

    /** 总页数 */
    private int totalPages;

    public static <T> PageResult<T> empty(int pageNo, int pageSize) {
        return PageResult.<T>builder()
                .list(Collections.emptyList())
                .total(0)
                .pageNo(pageNo)
                .pageSize(pageSize)
                .totalPages(0)
                .build();
    }
}
