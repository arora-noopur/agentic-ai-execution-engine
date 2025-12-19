package com.artc.agentic_ai_platform.taskqueue;

import com.artc.agentic_ai_platform.core.ITaskQueue;
import com.artc.agentic_ai_platform.model.Task;

import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class InMemoryQueue implements ITaskQueue {
    private final BlockingQueue<Task> queue = new LinkedBlockingQueue<>();
    public void push(Task t) { queue.offer(t); }
    public Optional<Task> pop() {
        try { return Optional.ofNullable(queue.poll(2, TimeUnit.SECONDS)); }
        catch (InterruptedException e) { return Optional.empty(); }
    }
}
