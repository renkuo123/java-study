package com.xyqb.platform.common.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

/**
 * RedisCacheService 单元测试
 *
 * <h3>测试策略</h3>
 * <p>
 * 使用 Mockito mock 掉 {@link RedisTemplate}，不需要真实的 Redis 服务。
 * 这样测试可以在任何环境下运行（CI/CD、本地无 Redis 的情况）。
 * </p>
 *
 * <h3>Java 测试框架说明</h3>
 * <ul>
 *     <li>{@code @ExtendWith(MockitoExtension.class)} — 启用 Mockito 注解支持。
 *         类比前端：类似 Jest 的 jest.mock() 机制，但通过注解自动完成。</li>
 *     <li>{@code @Mock} — 创建模拟对象（假的实现），调用其方法时不会执行真实逻辑，而是返回预设值。
 *         类比前端：类似 Jest 的 jest.fn() 创建的 mock 函数。</li>
 *     <li>{@code @Nested} — 按功能分组测试用例（内部类），让测试结构更清晰。</li>
 *     <li>{@code @DisplayName} — 给测试用例起一个可读的中文名称，在测试报告中显示。</li>
 * </ul>
 *
 * @author XYQB Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class RedisCacheServiceTest {

    /**
     * Mock 的 RedisTemplate，不会真正连接 Redis
     *
     * <p>
     * {@code @Mock} 注解会创建一个假的 RedisTemplate 实例，
     * 所有方法默认返回 null / 0 / false（根据返回类型）。
     * 我们可以用 {@code given(...).willReturn(...)} 来设置期望的返回值。
     * </p>
     */
    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * Mock 的 ValueOperations — Redis 字符串类型操作接口
     *
     * <p>
     * RedisTemplate.opsForValue() 返回的就是 ValueOperations 实例，
     * 通过它来执行 GET / SET 等命令。这里单独 mock 出来方便设置返回值。
     * </p>
     */
    @Mock
    private ValueOperations<String, Object> valueOperations;

    /** 被测试的缓存服务（手动构造，注入 mock 对象） */
    private RedisCacheService redisCacheService;

    /**
     * 每个测试方法执行前的初始化
     *
     * <p>
     * 这里没有用 {@code @InjectMocks}，因为 RedisCacheService 的构造函数
     * 需要 RedisTemplate 和 ObjectMapper 两个参数，ObjectMapper 不需要 mock
     * （它是一个纯工具类，直接 new 就行）。
     * </p>
     */
    @BeforeEach
    void setUp() {
        // 创建真实的 ObjectMapper（不需要 mock，它是无状态的工具类）
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        // 手动构造被测试的 Service，注入 mock 的 RedisTemplate 和真实的 ObjectMapper
        redisCacheService = new RedisCacheService(redisTemplate, objectMapper);
    }

    // ==================== set 方法测试 ====================

    @Nested
    @DisplayName("set - 写入缓存")
    class SetTests {

        @Test
        @DisplayName("正常写入缓存时应调用 RedisTemplate")
        void shouldCallRedisTemplateWithCorrectParams() {
            // given — 准备：让 redisTemplate.opsForValue() 返回我们 mock 的 valueOperations
            given(redisTemplate.opsForValue()).willReturn(valueOperations);

            // when — 执行：调用被测试的方法
            redisCacheService.set("test:key", "testValue", 30, TimeUnit.MINUTES);

            // then — 验证：检查 valueOperations.set() 是否被正确调用
            // verify() 是 Mockito 的验证方法，确认 mock 对象的某个方法被调用了
            // 类比前端：类似 Jest 的 expect(mockFn).toHaveBeenCalledWith(...)
            verify(valueOperations).set("test:key", "testValue", 30, TimeUnit.MINUTES);
        }

        @Test
        @DisplayName("Redis 异常时不应抛出异常（优雅降级）")
        void shouldNotThrowWhenRedisThrowsException() {
            // given — 让 opsForValue() 抛异常（模拟 Redis 宕机）
            given(redisTemplate.opsForValue()).willThrow(new RuntimeException("Redis 连接失败"));

            // when & then — 调用 set 不应该抛异常
            // 这里没有用 assertThatThrownBy，说明我们期望方法正常返回（不抛异常）
            redisCacheService.set("test:key", "testValue", 30, TimeUnit.MINUTES);
            // 如果走到这里没有异常，测试就通过了
        }
    }

    // ==================== get 方法测试 ====================

    @Nested
    @DisplayName("get - 读取单个对象")
    class GetTests {

        @Test
        @DisplayName("缓存命中时应返回正确类型的对象")
        void shouldReturnTypedObjectWhenCacheHit() {
            // given — 模拟 Redis 中存在数据
            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(valueOperations.get("test:key")).willReturn("cachedValue");

            // when
            String result = redisCacheService.get("test:key", String.class);

            // then
            assertThat(result).isEqualTo("cachedValue");
        }

        @Test
        @DisplayName("缓存未命中时应返回 null")
        void shouldReturnNullWhenCacheMiss() {
            // given — 模拟 Redis 中没有这个 Key
            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(valueOperations.get("nonexistent")).willReturn(null);

            // when
            String result = redisCacheService.get("nonexistent", String.class);

            // then — 缓存未命中返回 null，调用方会去查 MongoDB
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Redis 异常时应降级返回 null（不抛异常）")
        void shouldReturnNullWhenRedisThrowsException() {
            // given — 模拟 Redis 异常
            given(redisTemplate.opsForValue()).willThrow(new RuntimeException("Redis 超时"));

            // when
            String result = redisCacheService.get("test:key", String.class);

            // then — 降级返回 null
            assertThat(result).isNull();
        }
    }

    // ==================== getList 方法测试 ====================

    @Nested
    @DisplayName("getList - 读取列表数据")
    class GetListTests {

        @Test
        @DisplayName("缓存命中时应返回正确类型的列表")
        void shouldReturnTypedListWhenCacheHit() {
            // given — 模拟 Redis 中存储了一个 List
            // 注意：Redis 反序列化后可能返回 List<LinkedHashMap>，
            // 这里用简单的 String List 测试基本功能
            List<String> cachedList = List.of("item1", "item2", "item3");
            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(valueOperations.get("test:list")).willReturn(cachedList);

            // when
            List<String> result = redisCacheService.getList("test:list", String.class);

            // then
            assertThat(result).hasSize(3);
            assertThat(result).containsExactly("item1", "item2", "item3");
        }

        @Test
        @DisplayName("缓存未命中时应返回 null（不是空列表）")
        void shouldReturnNullWhenCacheMiss() {
            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(valueOperations.get("nonexistent")).willReturn(null);

            List<String> result = redisCacheService.getList("nonexistent", String.class);

            // 注意区分：null = 缓存未命中（需要查 DB），空列表 = DB 中确实没有数据
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Redis 异常时应降级返回 null")
        void shouldReturnNullWhenRedisThrowsException() {
            given(redisTemplate.opsForValue()).willThrow(new RuntimeException("Redis 连接断开"));

            List<String> result = redisCacheService.getList("test:list", String.class);

            assertThat(result).isNull();
        }
    }

    // ==================== delete 方法测试 ====================

    @Nested
    @DisplayName("delete - 删除缓存")
    class DeleteTests {

        @Test
        @DisplayName("Key 存在时应删除成功并返回 true")
        void shouldReturnTrueWhenKeyExists() {
            // given — 模拟删除成功
            given(redisTemplate.delete("test:key")).willReturn(true);

            // when
            boolean result = redisCacheService.delete("test:key");

            // then
            assertThat(result).isTrue();
            verify(redisTemplate).delete("test:key");
        }

        @Test
        @DisplayName("Key 不存在时应返回 false")
        void shouldReturnFalseWhenKeyNotExists() {
            given(redisTemplate.delete("nonexistent")).willReturn(false);

            boolean result = redisCacheService.delete("nonexistent");

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Redis 异常时不应抛出异常")
        void shouldNotThrowWhenRedisThrowsException() {
            given(redisTemplate.delete(anyString())).willThrow(new RuntimeException("Redis 异常"));

            boolean result = redisCacheService.delete("test:key");

            assertThat(result).isFalse();
        }
    }

    // ==================== hasKey 方法测试 ====================

    @Nested
    @DisplayName("hasKey - 检查 Key 是否存在")
    class HasKeyTests {

        @Test
        @DisplayName("Key 存在时应返回 true")
        void shouldReturnTrueWhenKeyExists() {
            given(redisTemplate.hasKey("test:key")).willReturn(true);

            boolean result = redisCacheService.hasKey("test:key");

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Key 不存在时应返回 false")
        void shouldReturnFalseWhenKeyNotExists() {
            given(redisTemplate.hasKey("nonexistent")).willReturn(false);

            boolean result = redisCacheService.hasKey("nonexistent");

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Redis 异常时应降级返回 false")
        void shouldReturnFalseWhenRedisThrowsException() {
            given(redisTemplate.hasKey(anyString())).willThrow(new RuntimeException("Redis 异常"));

            boolean result = redisCacheService.hasKey("test:key");

            assertThat(result).isFalse();
        }
    }
}
