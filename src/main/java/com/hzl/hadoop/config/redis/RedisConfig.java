package com.hzl.hadoop.config.redis;

import com.alibaba.fastjson.parser.ParserConfig;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.lang.Nullable;

import java.lang.reflect.Method;
import java.time.Duration;


/**
 * description
 * 1：springboot默认提供StringRedisTemplate和RedisTemplate,前者操作字符串的数据结构，后者你用JDK的序列化策略
 * 2：StringRedisTemplate继承了RedisTemplate
 * 3：配置redis的序列化策略，这里我们配置json的序列化，不然每个类都需要实现Serializable
 * 参考：https://blog.csdn.net/sz85850597/article/details/89301331
 * https://blog.csdn.net/zzhongcy/article/details/102584028?utm_medium=distribute.pc_relevant_bbs_down.none-task-blog-baidujs-1.nonecase&depth_1-utm_source=distribute.pc_relevant_bbs_down.none-task-blog-baidujs-1.nonecase
 *
 * @author hzl 2020/01/17 10:57 AM
 * @EnableCaching开启springboot的@cache注解
 */
@Slf4j
@Configuration
@EnableCaching
public class RedisConfig extends CachingConfigurerSupport {

	static {
		ParserConfig.getGlobalInstance().setAutoTypeSupport(true);
		ParserConfig.getGlobalInstance().addAccept("com.hzl.hadoop.app.dataobject");
	}


	/**
	 * <p>
	 * 当redis服务器发生异常时，忽略异常，然后返回空值，从而读取数据库
	 * 这样就不会因为redis异常导致服务无法正常使用
	 * ，虽然业务功能可以正常使用，但是mysql的压力会增加导致缓存雪崩（通过多层缓存方式，直接缓存到应用不合适，增加
	 * 其他缓存服务器成本较高，推荐方案redis集群，主从),小型erp系统基本不需要考虑
	 * </p>
	 *
	 * @author hzl 2020/01/17 3:18 PM
	 */
	@Override
	@Nullable
	public CacheErrorHandler errorHandler() {
		return new IgnoreExceptionCacheErrorHandler();
	}

	/**
	 * <p>
	 * 没有必要，完全可以在调用RedisTemplate的时候指定
	 * 生成key的策略 根据类名+方法名+所有参数的值生成唯一的一个key
	 *
	 * @Cacheable等注解可以手动指定该键生成规则 </p>
	 * @author hzl 2020/01/17 3:10 PM
	 */
	@Override
	@Bean
	public KeyGenerator keyGenerator() {
		//StringBuilder线程不安全，StringBuffer线程安全
		return new KeyGenerator() {
			@Override
			public Object generate(Object target, Method method, Object... params) {
				StringBuilder sb = new StringBuilder();
				sb.append(target.getClass().getName());
				sb.append(method.getName());
				for (Object obj : params) {
					sb.append(obj.toString());
				}
				return sb.toString();
			}
		};
	}


	public ObjectMapper objectMapper() {
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
		objectMapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance,
				ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
		objectMapper.registerModule(new JavaTimeModule())
				.registerModule(new ParameterNamesModule())
				.registerModule(new Jdk8Module());
		objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
		return objectMapper;
	}

	/**
	 * <p>
	 * 定义RedisTemplate存入redis时的key,value的序列化策略
	 * 使用jackson进行序列化
	 * 覆盖RedisAutoConfiguration
	 * 问题：无法处理日期格式
	 * </p>
	 *
	 * @author hzl 2020/01/17 3:10 PM
	 */
	@Bean(value = "redisTemplate")
	public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {

		//日期格式化结束
		Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer(Object.class);
		jackson2JsonRedisSerializer.setObjectMapper(objectMapper());

		RedisTemplate<String, Object> redisTemplate = new RedisTemplate<String, Object>();
		redisTemplate.setConnectionFactory(factory);
		//设置键的序列号为string
		redisTemplate.setHashKeySerializer(new StringRedisSerializer());
		redisTemplate.setKeySerializer(new StringRedisSerializer());
		//设置值的序列化为json
		redisTemplate.setHashValueSerializer(new StringRedisSerializer());
		redisTemplate.setValueSerializer(new StringRedisSerializer());

		return redisTemplate;
	}


	/**
	 * springboot的@Cacheable的redis缓存配置类
	 *
	 * @param redisConnectionFactory 连接工厂
	 * @return
	 * @author hzl 2020-01-17 9:29 PM
	 */

	@Bean
	public RedisCacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
		//初始化一个RedisCacheWriter
		RedisCacheWriter redisCacheWriter = RedisCacheWriter.nonLockingRedisCacheWriter(redisConnectionFactory);

		Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer(Object.class);
		jackson2JsonRedisSerializer.setObjectMapper(objectMapper());

		RedisSerializationContext.SerializationPair<Object> pair = RedisSerializationContext.SerializationPair
				.fromSerializer(jackson2JsonRedisSerializer);

		RedisCacheConfiguration defaultCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
				.serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
				.serializeValuesWith(pair).disableCachingNullValues();

		//设置默认超过期时间是30秒
		defaultCacheConfig.entryTtl(Duration.ofSeconds(30));
		//初始化RedisCacheManager
		return new RedisCacheManager(redisCacheWriter, defaultCacheConfig);

	}

}
