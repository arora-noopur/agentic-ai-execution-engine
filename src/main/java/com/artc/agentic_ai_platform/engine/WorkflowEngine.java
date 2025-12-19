package com.artc.agentic_ai_platform.engine;

import com.artc.agentic_ai_platform.config.AppConfig;
import com.artc.agentic_ai_platform.core.IAgent;
import com.artc.agentic_ai_platform.core.IStorageBackend;
import com.artc.agentic_ai_platform.core.ITaskQueue;
import com.artc.agentic_ai_platform.core.exception.RetryableException;
import com.artc.agentic_ai_platform.core.exception.TerminalException;
import com.artc.agentic_ai_platform.model.AgentType;
import com.artc.agentic_ai_platform.model.Task;
import com.artc.agentic_ai_platform.model.WorkflowStatus;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class WorkflowEngine {

    private final AppConfig appConfig;
    private final ITaskQueue queue;
    private final Map<AgentType, IAgent> agentMap;
    private final IStorageBackend storage;

    public WorkflowEngine(AppConfig appConfig, ITaskQueue queue, List<IAgent> agentList, IStorageBackend storage) {
        this.appConfig = appConfig;
        this.queue = queue;
        this.agentMap = agentList.stream().collect(Collectors.toMap(IAgent::getType, a -> a));
        this.storage = storage;
    }

    /**
     * The main consumer loop. This runs indefinitely on a worker thread.
     */
    public void runConsumerLoop(int workerId) {
        log.info("Worker-{} started.", workerId);

        while (true) {
            Task task = null;

            try {
                // 1. Check for Shutdown Signal before blocking
                if (Thread.currentThread().isInterrupted()) {
                    log.info("Worker-{} shutting down.", workerId);
                    break;
                }

                // 2. Fetch Task (Blocking)
                Optional<Task> taskOpt = queue.pop();
                if (taskOpt.isEmpty()) {
                    if (Thread.interrupted()) { // Clear flag if interrupted during poll
                        log.info("Worker-{} interrupted during poll. Shutting down.", workerId);
                        break;
                    }
                    continue;
                }

                task = taskOpt.get();
                MDC.put("traceId", task.getWorkflowId());

                // 3. Check Backoff (Is it too early?)
                if (shouldWait(task)) {
                    queue.push(task);
                    try { Thread.sleep(200); } catch (InterruptedException ig) {}
                    continue;
                }

                // 4. Process
                processTask(task);

            } catch (RetryableException e) {
                handleRetry(task, e);

            } catch (TerminalException e) {
                log.error("TERMINAL FAILURE [Task: {}]: {}. Dropping.",
                        task != null ? task.getTaskId() : "unknown", e.getMessage());

                // Handle Expected Fatal Errors and update workflow status accordingly
                if (task != null) markAsFailed(task.getWorkflowId(), e.getMessage());

            } catch (Exception e) {
                // Handle Shutdown or Crash
                if (isShutdownSignal(e)) {
                    log.info("Worker-{} received shutdown signal.", workerId);
                    Thread.currentThread().interrupt();
                    break;
                }
                log.error("Unexpected error processing task", e);

                // Handle unexpected crashes and update workflow status accordingly
                if (task != null) markAsFailed(task.getWorkflowId(), e.getMessage());

                // Prevent tight loop on crash
                try { Thread.sleep(1000); } catch (InterruptedException ig) {}
            } finally {
                MDC.clear();
            }
        }
    }

    private void processTask(Task task) {
        IAgent agent = agentMap.get(task.getTargetAgent());
        if (agent == null) {
            throw new TerminalException("Unknown Agent Type: " + task.getTargetAgent());
        }

        List<Task> downstreamTasks = agent.process(task);
        if (downstreamTasks != null) {
            downstreamTasks.forEach(queue::push);
        }
    }

    private void handleRetry(Task task, RetryableException e) {
        if (task == null) return;

        int maxRetries = appConfig.getQueue().getConcurrency().getMaxRetries();

        if (task.getRetryCount() >= maxRetries) {
            log.error("MAX RETRIES REACHED [Task: {}]. Dropping.", task.getTaskId());
            markAsFailed(task.getWorkflowId(), e.getMessage());
            return;
        }

        long delay = calculateDelay(task.getRetryCount());

        task.setRetryCount(task.getRetryCount() + 1);
        task.setNextRetryTimestamp(System.currentTimeMillis() + delay);

        log.info("Rescheduling [Task: {}] in {}ms (Attempt {}). Reason: {}",
                task.getTaskId(), delay, task.getRetryCount(), e.getMessage());

        queue.push(task);
    }

    private void markAsFailed(String workflowId, String reason) {
        try {
            log.warn("Setting status FAILED for workflow: {}", workflowId);
            storage.save("wf:" + workflowId + ":status", WorkflowStatus.FAILED.name());
            storage.save("wf:" + workflowId + ":error", reason);
        } catch (Exception e) {
            log.error("Failed to write FAILED status to storage for {}", workflowId, e);
        }
    }

    private long calculateDelay(int currentRetries) {
        String strategy = appConfig.getQueue().getConcurrency().getBackoffStrategy();
        long baseDelayMs = 1000L;

        if("fixed".equalsIgnoreCase(strategy)) {
            return baseDelayMs;
        } else {
            return baseDelayMs * (long) Math.pow(2, currentRetries);
        }
    }

    private boolean shouldWait(Task task) {
        return task.getNextRetryTimestamp() > 0 &&
                System.currentTimeMillis() < task.getNextRetryTimestamp();
    }

    private boolean isShutdownSignal(Exception e) {
        return e instanceof InterruptedException || e.getCause() instanceof InterruptedException;
    }
}