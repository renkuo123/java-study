package com.xyqb.platform.module.category.repository;

import com.xyqb.platform.module.category.entity.Category;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * 分类数据访问层（Repository）
 *
 * <p>继承 {@link MongoRepository}，自动获得 CRUD 基础操作：
 * <ul>
 *     <li>{@code save()} - 新增或更新文档</li>
 *     <li>{@code findById()} - 根据 ID 查询</li>
 *     <li>{@code findAll()} - 查询所有文档</li>
 *     <li>{@code deleteById()} - 根据 ID 删除</li>
 *     <li>{@code existsById()} - 判断文档是否存在</li>
 * </ul>
 *
 * <p>Spring Data MongoDB 的查询方法命名规则：
 * 按照约定的方法名格式定义接口方法，框架会自动生成 MongoDB 查询语句，无需手写实现。
 *
 * <p>泛型参数说明：
 * <ul>
 *     <li>{@code Category} - 操作的文档实体类型</li>
 *     <li>{@code String} - 文档 ID 的类型（MongoDB ObjectId 映射为 String）</li>
 * </ul>
 *
 * @author XYQB Team
 * @since 1.0.0
 */
@Repository
public interface CategoryRepository extends MongoRepository<Category, String> {
}
