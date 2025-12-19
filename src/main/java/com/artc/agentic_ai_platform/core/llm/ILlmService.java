package com.artc.agentic_ai_platform.core.llm;

public interface ILlmService {
    public String generate(String systemPrompt, String userPrompt);
}
