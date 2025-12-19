package com.artc.agentic_ai_platform.engine;

import com.artc.agentic_ai_platform.config.AppConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executor;

@Component
@Slf4j
public class WorkerBootstrap {

    private final WorkflowEngine workflowEngine;
    private final Executor executor;

    private final AppConfig appConfig;

    public WorkerBootstrap(AppConfig appConfig, WorkflowEngine workflowEngine, @Qualifier("globalQueueExecutor") Executor executor) {
        this.appConfig = appConfig;
        this.workflowEngine = workflowEngine;
        this.executor = executor;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {

        int workerCount = appConfig.getQueue().getConcurrency().getWorkers();
        log.info(">> BOOTSTRAP: Launching {} worker threads ...", workerCount);

        for(int i=0;i < workerCount;i++) {
            final int workerId = i;
            executor.execute(() -> workflowEngine.runConsumerLoop(workerId));
        }
    }
}
