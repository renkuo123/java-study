package com.xyqb.platform.common.config;

/*
 * ========== import 导入说明 ==========
 *
 * Java 的 import 类似前端的 import/require，用于引入其他包中的类。
 * 不同于 JavaScript 可以 import 任意导出，Java 的 import 只能导入"类"（class/interface/enum）。
 *
 * 前端类比：
 *   JavaScript: import { RedisTemplate } from 'spring-data-redis';
 *   Java:       import org.springframework.data.redis.core.RedisTemplate;
 *
 * Java 的包名是用点号（.）分隔的，对应文件系统的目录结构：
 *   org.springframework.data.redis.core.RedisTemplate
 *   → org/springframework/data/redis/core/RedisTemplate.java
 */
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 配置类 — 自定义 RedisTemplate 的序列化方式
 *
 * <h3>为什么需要这个配置？</h3>
 * <p>
 * Spring Boot 自动配置的 {@link RedisTemplate} 默认使用 JDK 序列化（{@code JdkSerializationRedisSerializer}），
 * 存入 Redis 的数据是二进制格式，用 redis-cli 查看时显示为乱码，调试非常不方便。
 * </p>
 *
 * <p>
 * 我们把它改成 JSON 序列化，这样在 Redis 中存储的数据是可读的 JSON 字符串，
 * 就像前端用 {@code JSON.stringify()} 把对象转成字符串存入 localStorage 一样。
 * </p>
 *
 * <h3>Java 语法说明</h3>
 * <ul>
 *     <li>{@code @Configuration} — 标记这是一个配置类，Spring 启动时会扫描并执行其中的 @Bean 方法。
 *         类比前端：相当于 Vue 的 app.use(plugin) 或 React 的 Provider 配置。</li>
 *     <li>{@code @Bean} — 标记方法的返回值会被注册到 Spring IoC 容器中，其他地方可以通过依赖注入获取。
 *         类比前端：相当于一个工厂函数，返回的实例会被全局共享（单例模式）。</li>
 * </ul>
 *
 * @author XYQB Team
 * @since 1.0.0
 */
@Configuration  // 告诉 Spring：这个类是一个配置类，请在启动时加载它
public class RedisConfig {

    /**
     * 创建并配置自定义的 RedisTemplate
     *
     * <h3>什么是 RedisTemplate？</h3>
     * <p>
     * RedisTemplate 是 Spring 提供的 Redis 操作工具类，封装了所有 Redis 命令。
     * 类比前端：它就像 axios 实例，你可以配置拦截器（序列化器），然后用它发请求（Redis 命令）。
     * </p>
     *
     * <h3>泛型 {@code <String, Object>} 是什么意思？</h3>
     * <p>
     * {@code RedisTemplate<String, Object>} 表示：
     * - 第一个类型参数 String：Redis 的 Key（键）是字符串类型
     * - 第二个类型参数 Object：Redis 的 Value（值）可以是任意 Java 对象
     *
     * 类比 TypeScript：{@code RedisTemplate<string, any>}
     * </p>
     *
     * @param connectionFactory Redis 连接工厂，由 Spring Boot 根据 yml 配置自动创建并注入。
     *                          类比前端：就像 axios.create() 时传入的 baseURL 配置，
     *                          connectionFactory 封装了 Redis 的地址、端口、密码等连接信息。
     * @return 配置好序列化方式的 RedisTemplate 实例
     */
    @Bean  // 这个注解告诉 Spring：请把这个方法的返回值注册为一个 Bean（可注入的对象）
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {

        // ====== 第一步：创建 RedisTemplate 实例 ======
        // new 关键字：在堆内存中创建一个对象（类比 JS 的 new Map() 或 new axios()）
        // 菱形语法 <>：Java 7 引入，编译器会自动推断泛型类型，不用重复写 <String, Object>
        RedisTemplate<String, Object> template = new RedisTemplate<>();

        // ====== 第二步：设置连接工厂 ======
        // 告诉 template 用哪个连接去访问 Redis（类似 axios 实例绑定 baseURL）
        template.setConnectionFactory(connectionFactory);

        // ====== 第三步：配置 JSON 序列化器 ======
        // 序列化 = 把 Java 对象变成可以存储/传输的格式（类比前端的 JSON.stringify）
        // 反序列化 = 把存储格式还原成 Java 对象（类比前端的 JSON.parse）
        GenericJackson2JsonRedisSerializer jsonSerializer = createJsonSerializer();

        // ====== 第四步：分别设置 Key 和 Value 的序列化方式 ======

        // Key 的序列化器：用字符串序列化（StringRedisSerializer）
        // 因为 Redis 的 Key 应该是人类可读的字符串，比如 "category:list"
        // new StringRedisSerializer() 就是直接把 String 按 UTF-8 编码存储
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);          // 普通 Key 的序列化
        template.setHashKeySerializer(stringSerializer);          // Hash 类型的 field 序列化

        // Value 的序列化器：用 JSON 序列化（GenericJackson2JsonRedisSerializer）
        // 这样存入 Redis 的值是 JSON 字符串，用 redis-cli 能直接看到内容
        template.setValueSerializer(jsonSerializer);           // 普通 Value 的序列化
        template.setHashValueSerializer(jsonSerializer);       // Hash 类型的 value 序列化

        // ====== 第五步：使配置生效 ======
        // afterPropertiesSet() 是 Spring 的初始化回调，确保所有属性都设置好后再使用
        // 类似前端组件的 mounted/useEffect，在所有配置就绪后执行初始化逻辑
        template.afterPropertiesSet();

        return template;
    }

    /**
     * 创建配置好的 JSON 序列化器
     *
     * <h3>为什么要单独配置 ObjectMapper？</h3>
     * <p>
     * Jackson 的 ObjectMapper 是 JSON 处理的核心类（类比前端的 JSON 对象，但功能更强大）。
     * 默认的 ObjectMapper 不支持 Java 8 的日期类型（{@code LocalDateTime}），
     * 而我们的 {@code BaseEntity} 中的 {@code createdAt} 和 {@code updatedAt} 就是这个类型，
     * 如果不注册 JavaTimeModule，序列化时会报错。
     * </p>
     *
     * <h3>private 访问修饰符</h3>
     * <p>
     * {@code private} 表示这个方法只能在当前类内部调用，外部不可见。
     * Java 的访问控制从严到松：private → default(不写) → protected → public
     * 类比 TypeScript 的 private 关键字，作用完全相同。
     * </p>
     *
     * @return 配置好的 JSON 序列化器
     */
    private GenericJackson2JsonRedisSerializer createJsonSerializer() {
        // 创建 Jackson 的 ObjectMapper（JSON 序列化/反序列化的核心引擎）
        ObjectMapper mapper = new ObjectMapper();

        // 注册 Java 8 时间模块，让 ObjectMapper 能正确处理 LocalDateTime 等类型
        // 不注册的话，序列化 LocalDateTime 会报错：
        //   "Java 8 date/time type `java.time.LocalDateTime` not supported by default"
        // 类比前端：dayjs 也需要 import 插件才能处理某些日期格式
        mapper.registerModule(new JavaTimeModule());

        // 禁止把日期类型序列化为时间戳数字（如 1711900800000）
        // 改为输出 ISO 8601 格式的字符串（如 "2024-04-01T12:00:00"），更易读
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // 使用配置好的 mapper 创建 JSON 序列化器
        // GenericJackson2JsonRedisSerializer 会在 JSON 中自动添加 @class 字段，
        // 记录对象的 Java 类型信息，这样反序列化时能知道要还原成哪个类
        // 例如存入 Redis 的 JSON 会包含：{"@class": "com.xyqb.platform.module.category.entity.Category", ...}
        return new GenericJackson2JsonRedisSerializer(mapper);
    }
}
