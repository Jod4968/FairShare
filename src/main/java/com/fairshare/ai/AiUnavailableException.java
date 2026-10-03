package com.fairshare.ai;

public class AiUnavailableException extends RuntimeException {
    public AiUnavailableException() { super("AI assistant is currently unavailable."); }
}
