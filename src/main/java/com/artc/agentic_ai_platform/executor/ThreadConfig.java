package com.artc.agentic_ai_platform.executor;

import com.artc.agentic_ai_platform.config.AppConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@RequiredArgsConstructor
public class ThreadConfig {

    private final AppConfig appConfig;

    // 1. GLOBAL QUEUE EXECUTOR
    // This pool is used by WorkerEngine to pull and execute tasks from main queue.
    @Bean(name = "globalQueueExecutor")
    public Executor globalQueueExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        int workers = appConfig.getQueue().getConcurrency().getWorkers();

        // CONCURRENCY SETTINGS
        // Core = Max ensures we always have exactly 'poolSize' threads active
        executor.setCorePoolSize(workers);
        executor.setMaxPoolSize(workers);

        // Queue Capacity = 0 , Do not queue tasks in the thread pool itself.
        // We want threads to be active immediately.
        executor.setQueueCapacity(0);
        executor.setThreadNamePrefix("GlobalQ-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
     }

     // 2. WORKER AGENT INTERNAL POOL
     // This pool is used by WorkerAgent for parallel sub-task processing
     @Bean(name = "workerInternalExecutor")
     public Executor workerInternalExecutor() {

        int poolSize = appConfig.getAgents().getWorkers().getPoolSize();

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(poolSize);
        executor.setMaxPoolSize(poolSize);
        executor.setThreadNamePrefix("InnerWork-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
     }
}
