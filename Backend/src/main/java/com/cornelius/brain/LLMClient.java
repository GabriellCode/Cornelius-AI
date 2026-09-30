package com.cornelius.brain;

import java.util.List;

public interface LLMClient {
    String generate(String systemPrompt, String userPrompt, List<ChatMessage> history) throws Exception;
    boolean isAvailable();
    String getProviderName();
}

