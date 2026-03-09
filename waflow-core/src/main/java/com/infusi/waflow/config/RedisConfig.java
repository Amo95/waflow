package com.infusi.waflow.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.infusi.waflow.session.InMemorySessionStore;
import com.infusi.waflow.session.RedisSessionStore;
import com.infusi.waflow.session.SessionStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration
public class RedisConfig {

    @Bean
    @ConditionalOnClass(RedisConnectionFactory.class)
    @ConditionalOnBean(RedisConnectionFactory.class)
    public RedisTemplate<String, String> waflowRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    @ConditionalOnBean(RedisTemplate.class)
    public SessionStore redisSessionStore(RedisTemplate<String, String> redisTemplate,
                                          ObjectMapper objectMapper,
                                          @Value("${waflow.session.timeout:PT24H}") Duration timeout) {
        return new RedisSessionStore(redisTemplate, objectMapper, timeout);
    }

    @Bean
    @ConditionalOnMissingBean(SessionStore.class)
    public SessionStore inMemorySessionStore() {
        return new InMemorySessionStore();
    }
}
