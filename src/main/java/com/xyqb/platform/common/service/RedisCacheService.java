package com.xyqb.platform.common.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis 缓存通用工具类 — 封装常用的缓存读写操作
 *
 * <h3>设计理念</h3>
 * <p>
 * 这个类把 {@link RedisTemplate} 的底层 API 封装成更简单易用的方法，
 * 就像前端经常封装 {@code localStorage} 的读写一样：
 * </p>
 * <pre>
 * // 前端封装 localStorage 的常见写法：
 * const cache = {
 *   set(key, value, ttl) { localStorage.setItem(key, JSON.stringify(value)); },
 *   get(key) { return JSON.parse(localStorage.getItem(key)); },
 *   delete(key) { localStorage.removeItem(key); }
 * };
 * </pre>
 * <p>
 * 这个类做的事情完全一样，只不过存储介质从浏览器的 localStorage 变成了 Redis 服务器。
 * </p>
 *
 * <h3>异常处理策略（降级设计）</h3>
 * <p>
 * <b>读操作（get / getList / hasKey）：</b>Redis 异常时返回 null/false，让业务层自动"穿透"到 MongoDB 查询。
 * 这样即使 Redis 宕机了，系统还能正常运行（只是慢一点），这叫"优雅降级"。
 * </p>
 * <p>
 * <b>写操作（set / delete）：</b>Redis 异常时只记日志不抛异常。
 * 因为写操作的主逻辑（存 MongoDB）已经完成了，缓存写失败不影响数据正确性，
 * 最多导致下次读取时缓存未命中而已。
 * </p>
 *
 * <h3>Java 语法说明</h3>
 * <ul>
 *     <li>{@code @Service} — 标记这是一个"服务层"的 Bean（可注入的组件）。
 *         功能上和 @Component 完全一样，但语义更清晰，表示这是业务/工具服务。
 *         类比前端：Vue 的 provide/inject 或 Angular 的 @Injectable。</li>
 *     <li>{@code @Slf4j} — Lombok 注解，自动生成一个名为 log 的日志对象。
 *         等价于手写：{@code private static final Logger log = LoggerFactory.getLogger(RedisCacheService.class);}</li>
 *     <li>{@code @RequiredArgsConstructor} — Lombok 注解，自动生成一个包含所有 final 字段的构造函数。
 *         Spring 会通过这个构造函数自动注入 RedisTemplate（构造器注入，比 @Autowired 字段注入更推荐）。</li>
 * </ul>
 *
 * @author XYQB Team
 * @since 1.0.0
 */
@Slf4j                       // 自动生成 log 日志对象
@Service                     // 注册为 Spring Bean，其他类可以通过依赖注入获取这个实例
@RequiredArgsConstructor     // 自动生成构造函数：RedisCacheService(RedisTemplate<String, Object> redisTemplate)
public class RedisCacheService {

    /**
     * Redis 操作模板，由 RedisConfig 中配置的 Bean 自动注入
     *
     * <h3>private final 是什么意思？</h3>
     * <ul>
     *     <li>{@code private} — 只能在本类内部访问（封装性，外部不能直接操作 redisTemplate）</li>
     *     <li>{@code final} — 引用一旦赋值就不能再改变（类似 JS 的 const）。
     *         注意：final 只保证引用不变，对象内部状态仍然可以改变
     *         （就像 JS 的 const obj = {}; obj.a = 1; 是合法的）。</li>
     * </ul>
     *
     * <p>
     * 为什么用 private final 而不是 @Autowired？
     * 因为 final 字段必须在构造函数中赋值，配合 @RequiredArgsConstructor，
     * Spring 会在创建 RedisCacheService 实例时通过构造函数注入 redisTemplate。
     * 这种方式更安全（不可变）、更容易测试（构造函数参数一目了然）。
     * </p>
     */
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * Jackson ObjectMapper，用于列表类型的反序列化
     *
     * <p>
     * 为什么需要它？因为 Redis 存储的 JSON 在反序列化 List 时，
     * Jackson 可能返回 LinkedHashMap 而不是具体的实体类。
     * 我们需要用 ObjectMapper 手动做类型转换。
     * </p>
     */
    private final ObjectMapper objectMapper;

    // ==================== 写操作 ====================

    /**
     * 将数据存入 Redis 缓存，并设置过期时间
     *
     * <h3>方法签名解读</h3>
     * <pre>
     * public void set(String key, Object value, long timeout, TimeUnit unit)
     * │      │    │   │          │             │              │
     * │      │    │   │          │             │              └─ 参数4：时间单位枚举
     * │      │    │   │          │             └─ 参数3：过期时间的数值（long 是 64 位整数）
     * │      │    │   │          └─ 参数2：要缓存的值（Object 是所有类的父类，类似 TS 的 any）
     * │      │    │   └─ 参数1：缓存的键
     * │      │    └─ 方法名
     * │      └─ 返回类型 void（无返回值，类似 TS 的 void）
     * └─ 访问修饰符（公开的，任何地方都能调用）
     * </pre>
     *
     * <h3>类比前端</h3>
     * <pre>
     * // 前端等价写法（带过期时间的 localStorage）：
     * function set(key: string, value: any, timeout: number, unit: 'SECONDS' | 'MINUTES') {
     *   const ttlMs = unit === 'MINUTES' ? timeout * 60000 : timeout * 1000;
     *   localStorage.setItem(key, JSON.stringify({ value, expiry: Date.now() + ttlMs }));
     * }
     * </pre>
     * <p>Redis 原生支持过期时间，不需要自己计算，比前端方便很多。</p>
     *
     * @param key     缓存键，如 "category:list"
     * @param value   缓存值，可以是任意 Java 对象（会被 JSON 序列化后存入 Redis）
     * @param timeout 过期时间数值
     * @param unit    过期时间单位（如 TimeUnit.MINUTES, TimeUnit.SECONDS）
     */
    public void set(String key, Object value, long timeout, TimeUnit unit) {
        // try-catch 异常处理：类比 JS 的 try { ... } catch (error) { ... }
        // Java 的异常处理语法和 JavaScript 几乎一样
        try {
            // opsForValue() 获取"字符串类型"的操作对象（Redis 有 5 种数据类型，这里用最基本的 String）
            // .set(key, value, timeout, unit) 相当于 Redis 命令：SET key value EX timeout
            redisTemplate.opsForValue().set(key, value, timeout, unit);
            log.debug("缓存写入成功: key={}", key);
        } catch (Exception e) {
            // 写缓存失败不抛异常，只记日志
            // 因为主要数据已经存入 MongoDB 了，缓存只是加速手段
            log.error("缓存写入失败: key={}, error={}", key, e.getMessage(), e);
        }
    }

    // ==================== 读操作 ====================

    /**
     * 从 Redis 缓存中读取单个对象
     *
     * <h3>泛型方法 {@code <T>} 说明</h3>
     * <pre>
     * public &lt;T&gt; T get(String key, Class&lt;T&gt; clazz)
     * │       │  │  │             │
     * │       │  │  │             └─ Class&lt;T&gt; 类型令牌：告诉方法要把结果转成什么类型
     * │       │  │  └─ 方法名
     * │       │  └─ 返回类型 T（跟随调用者指定的类型）
     * │       └─ 泛型声明：声明这个方法使用了一个叫 T 的类型参数
     * └─ 访问修饰符
     * </pre>
     *
     * <p>
     * 类比 TypeScript：
     * <pre>
     * function get&lt;T&gt;(key: string, clazz: new () =&gt; T): T | null {
     *   const raw = localStorage.getItem(key);
     *   return raw ? JSON.parse(raw) as T : null;
     * }
     * </pre>
     * </p>
     *
     * <h3>为什么需要 Class&lt;T&gt; 参数？</h3>
     * <p>
     * Java 有"类型擦除"（Type Erasure）机制：泛型信息在编译后会被擦除，
     * 运行时不知道 T 具体是什么类型。所以需要显式传入 Class 对象来告诉方法目标类型。
     * TypeScript 没有这个问题，因为 TS 的泛型是编译时检查，运行时用的是 JS 对象。
     * </p>
     *
     * @param key   缓存键
     * @param clazz 目标类型的 Class 对象（如 {@code Category.class}）
     * @param <T>   返回值的类型
     * @return 缓存中的对象，如果不存在或 Redis 异常则返回 null
     */
    public <T> T get(String key, Class<T> clazz) {
        try {
            // opsForValue().get(key) 相当于 Redis 命令：GET key
            // 返回值是 Object 类型（因为 RedisTemplate 的 Value 泛型是 Object）
            Object value = redisTemplate.opsForValue().get(key);

            // 如果缓存中没有这个 key，返回 null
            if (value == null) {
                return null;
            }

            // 类型转换：把 Object 转换为目标类型 T
            // objectMapper.convertValue() 类似前端的类型断言，但更安全——它会实际检查并转换数据
            return objectMapper.convertValue(value, clazz);
        } catch (Exception e) {
            // 读缓存失败时"降级"：返回 null，让调用方去查 MongoDB
            // 这就是"优雅降级"——Redis 挂了不影响系统正常运行，只是少了缓存加速
            log.warn("缓存读取失败（将降级查库）: key={}, error={}", key, e.getMessage());
            return null;
        }
    }

    /**
     * 从 Redis 缓存中读取列表数据
     *
     * <h3>为什么单独写一个 getList 方法？</h3>
     * <p>
     * 因为 Java 的泛型擦除问题：{@code List<Category>} 在运行时变成了 {@code List}，
     * Jackson 反序列化时不知道列表中的元素应该是什么类型，会默认转成 {@code LinkedHashMap}。
     * 所以需要额外处理，逐个元素转换类型。
     * </p>
     * <p>
     * 这个问题在 TypeScript 中不存在，因为 TS 不会擦除类型信息。
     * </p>
     *
     * @param key   缓存键
     * @param clazz 列表中元素的类型（如 {@code Category.class}）
     * @param <T>   列表元素的类型
     * @return 缓存中的列表，如果不存在或 Redis 异常则返回 null（注意不是空列表）
     */
    public <T> List<T> getList(String key, Class<T> clazz) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value == null) {
                return null;
            }

            // 如果取出来已经是 List 类型
            if (value instanceof List<?> rawList) {
                // instanceof 运算符：检查对象是否属于某个类型（类似 JS 的 instanceof）
                // Java 16 新语法 "instanceof List<?> rawList"：在检查类型的同时声明变量
                // <?> 是通配符泛型：表示"任意类型的 List"（因为此时还不知道元素具体类型）

                // 用 stream 逐个转换元素类型
                // .stream() 把 List 转成 Stream（类似 JS 数组的链式调用 .map().filter()）
                // .map() 对每个元素执行转换（完全等价于 JS 的 Array.map()）
                // .toList() 收集结果为新的 List（类似 JS 中 map 自动返回新数组）
                return rawList.stream()
                        .map(item -> objectMapper.convertValue(item, clazz))
                        .toList();
            }

            // 如果类型不匹配，返回 null
            return null;
        } catch (Exception e) {
            log.warn("缓存列表读取失败（将降级查库）: key={}, error={}", key, e.getMessage());
            return null;
        }
    }

    // ==================== 删除操作 ====================

    /**
     * 删除指定的缓存 key
     *
     * <h3>Boolean vs boolean</h3>
     * <p>
     * Java 有"基本类型"和"包装类型"的区别：
     * <ul>
     *     <li>{@code boolean}（小写）— 基本类型，值只能是 true/false，不能是 null</li>
     *     <li>{@code Boolean}（大写）— 包装类型（对象），值可以是 true/false/null</li>
     * </ul>
     * {@code redisTemplate.delete()} 返回的是 {@code Boolean}（可能为 null），
     * 我们需要用 {@code Boolean.TRUE.equals()} 来安全地比较，避免空指针异常。
     * </p>
     * <p>
     * JavaScript 中没有这个问题，因为 JS 的 boolean 本身就可以是 undefined。
     * </p>
     *
     * @param key 要删除的缓存键
     * @return true 表示删除成功，false 表示 key 不存在或删除失败
     */
    public boolean delete(String key) {
        try {
            // redisTemplate.delete(key) 相当于 Redis 命令：DEL key
            // 返回 Boolean（注意大写），可能为 null
            Boolean result = redisTemplate.delete(key);

            // Boolean.TRUE.equals(result) 是 null 安全的比较方式：
            // - result 为 true → 返回 true
            // - result 为 false 或 null → 返回 false
            // 如果直接写 result == true，当 result 为 null 时会触发拆箱（unboxing）导致 NullPointerException
            boolean deleted = Boolean.TRUE.equals(result);

            if (deleted) {
                log.debug("缓存删除成功: key={}", key);
            }
            return deleted;
        } catch (Exception e) {
            // 删缓存失败只记日志，不影响主业务
            // 最坏情况：缓存中残留了旧数据，但 TTL 到期后会自动清除
            log.error("缓存删除失败: key={}, error={}", key, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 检查缓存中是否存在指定的 key
     *
     * @param key 要检查的缓存键
     * @return true 表示存在，false 表示不存在或 Redis 异常
     */
    public boolean hasKey(String key) {
        try {
            // hasKey() 相当于 Redis 命令：EXISTS key
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            log.warn("缓存 key 检查失败: key={}, error={}", key, e.getMessage());
            return false;
        }
    }
}
