package com.artc.agentic_ai_platform.core;

import com.artc.agentic_ai_platform.model.Task;

import java.util.Optional;

public interface ITaskQueue {
    void push(Task task);
    Optional<Task> pop(); // Blocking pop
}