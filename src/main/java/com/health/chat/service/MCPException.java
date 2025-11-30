package com.health.chat.service;

/**
 * Exception thrown when MCP service operations fail.
 */
public class MCPException extends Exception {
    
    public MCPException(String message) {
        super(message);
    }
    
    public MCPException(String message, Throwable cause) {
        super(message, cause);
    }
}
