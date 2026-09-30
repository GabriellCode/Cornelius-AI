package com.cornelius.brain.tools;

public interface AgentTool {
    String getName();
    String getDescription();
    String execute(String input) throws Exception;
}

