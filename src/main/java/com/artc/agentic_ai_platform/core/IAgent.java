package com.artc.agentic_ai_platform.core;


import com.artc.agentic_ai_platform.model.AgentType;
import com.artc.agentic_ai_platform.model.Task;

import java.util.List;

public interface IAgent {
    AgentType getType();
    List<Task> process(Task task);
}
