package com.artc.agentic_ai_platform.infra;



import com.artc.agentic_ai_platform.config.AppConfig;
import com.artc.agentic_ai_platform.core.IStorageBackend;
import com.artc.agentic_ai_platform.core.ITaskQueue;
import com.artc.agentic_ai_platform.model.Task;
import com.artc.agentic_ai_platform.storage.RamStorage;
import com.artc.agentic_ai_platform.storage.RedisStorage;
import com.artc.agentic_ai_platform.taskqueue.InMemoryQueue;
import com.artc.agentic_ai_platform.taskqueue.RedisQueue;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@Slf4j
public class InfrastructureConfig {

    // --- REDIS SETUP ---
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        return template;
    }

    // --- STRATEGY: IN-MEMORY (Default) ---
    @Bean
    @ConditionalOnProperty(name = "app.storage.backend", havingValue = "ram", matchIfMissing = true)
    public IStorageBackend ramStorage(AppConfig appConfig) {
        log.info(">> STORAGE: RAM (Volatile)");
        return new RamStorage(appConfig);
    }

    @Bean
    @ConditionalOnProperty(name = "app.queue.backend", havingValue = "inmem", matchIfMissing = true)
    public ITaskQueue inMemoryQueue() {
        log.info(">> QUEUE: RAM (Local)");
        return new InMemoryQueue();
    }

    // --- STRATEGY: REDIS (Production) ---
    @Bean
    @ConditionalOnProperty(name = "app.storage.backend", havingValue = "redis")
    public IStorageBackend redisStorage(RedisTemplate<String, Object> template, ObjectMapper mapper, AppConfig appConfig) {
        log.info(">> STORAGE: REDIS (Persistent)");
        return new RedisStorage(template, mapper, appConfig);
    }

    @Bean
    @ConditionalOnProperty(name = "app.queue.backend", havingValue = "redis")
    public ITaskQueue redisQueue(RedisTemplate<String, Object> template, ObjectMapper mapper) {
        log.info(">> QUEUE: REDIS (Distributed)");
        return new RedisQueue(template, mapper);
    }
}
