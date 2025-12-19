package com.artc.agentic_ai_platform.taskqueue;

import com.artc.agentic_ai_platform.core.ITaskQueue;
import com.artc.agentic_ai_platform.model.Task;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
public class RedisQueue implements ITaskQueue {
    private final RedisTemplate<String, Object> template;
    private final ObjectMapper mapper;

    public void push(Task t) {
        template.opsForList().leftPush("task_queue", t);
    }

    public Optional<Task> pop() {
        Object t = template.opsForList().rightPop("task_queue", 2, TimeUnit.SECONDS);
        if (t == null) return Optional.empty();
        // Handle Jackson deserialization if Redis returns raw map
        try { return Optional.of(mapper.convertValue(t, Task.class)); }
        catch (Exception e) { return Optional.of((Task) t); }
    }
}
