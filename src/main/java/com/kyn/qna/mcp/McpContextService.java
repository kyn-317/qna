package com.kyn.qna.mcp;

import com.kyn.qna.model.Answer;
import com.kyn.qna.model.Question;
import io.modelcontextprotocol.core.Context;
import io.modelcontextprotocol.core.Fact;
import io.modelcontextprotocol.core.Identity;
import io.modelcontextprotocol.core.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class McpContextService {

    private static final Logger logger = LoggerFactory.getLogger(McpContextService.class);
    private final Map<String, Context> activeContexts = new ConcurrentHashMap<>();

    public String initializeContext(String sessionId, String technologyStack, String experienceLevel, String userId) {
        Identity identity = new Identity();
        identity.setSubject(userId);
        // Assuming other Identity fields are optional or set by default

        List<Fact> facts = new ArrayList<>();
        facts.add(new Fact("technologyStack", technologyStack));
        facts.add(new Fact("experienceLevel", experienceLevel));

        Context context = new Context();
        context.setIdentity(identity);
        context.setFacts(facts);
        context.setMessages(new ArrayList<>()); // Initialize with an empty list of messages

        activeContexts.put(sessionId, context);
        logger.info("Initialized MCP Context for session ID: {} with user ID: {}", sessionId, userId);
        return sessionId;
    }

    public Context getContext(String sessionId) {
        Context context = activeContexts.get(sessionId);
        if (context == null) {
            logger.warn("MCP Context not found for session ID: {}", sessionId);
            // Depending on requirements, could throw an exception here:
            // throw new McpContextNotFoundException("Context not found for session ID: " + sessionId);
        }
        return context;
    }

    public void addQuestionToContext(String sessionId, Question question) {
        Context context = getContext(sessionId);
        if (context == null) {
            // getContext already logs a warning, could throw specific exception if needed
            logger.error("Cannot add question to context. MCP Context not found for session ID: {}", sessionId);
            // Or throw new McpContextNotFoundException("Cannot add question. Context not found for session ID: " + sessionId);
            return;
        }

        Message message = new Message();
        message.setRole("assistant"); // System-generated question
        message.setContent(question.getText());
        // Assuming Message has a way to set timestamp or it's handled by MCP SDK

        // Ensure messages list is not null
        if (context.getMessages() == null) {
            context.setMessages(new ArrayList<>());
        }
        context.getMessages().add(message);
        logger.info("Added question to MCP Context for session ID: {}", sessionId);
    }

    public void addAnswerToContext(String sessionId, Answer answer) {
        Context context = getContext(sessionId);
        if (context == null) {
            logger.error("Cannot add answer to context. MCP Context not found for session ID: {}", sessionId);
            // Or throw new McpContextNotFoundException("Cannot add answer. Context not found for session ID: " + sessionId);
            return;
        }

        Message message = new Message();
        message.setRole("user"); // User's answer
        message.setContent(answer.getText());

        if (context.getMessages() == null) {
            context.setMessages(new ArrayList<>());
        }
        context.getMessages().add(message);
        logger.info("Added answer to MCP Context for session ID: {}", sessionId);
    }

    public List<Fact> getFacts(String sessionId) {
        Context context = getContext(sessionId);
        if (context == null) {
            logger.warn("Cannot get facts. MCP Context not found for session ID: {}", sessionId);
            return List.of(); // Return empty list if context not found
        }
        return Optional.ofNullable(context.getFacts()).orElse(List.of());
    }

    public List<Message> getMessages(String sessionId) {
        Context context = getContext(sessionId);
        if (context == null) {
            logger.warn("Cannot get messages. MCP Context not found for session ID: {}", sessionId);
            return List.of(); // Return empty list if context not found
        }
        return Optional.ofNullable(context.getMessages()).orElse(List.of());
    }
    
    // Optional: Custom exception for context not found
    // public static class McpContextNotFoundException extends RuntimeException {
    //     public McpContextNotFoundException(String message) {
    //         super(message);
    //     }
    // }
}
