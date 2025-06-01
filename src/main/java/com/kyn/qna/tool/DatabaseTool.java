package com.kyn.qna.tool;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kyn.qna.service.QuestionService;
import com.kyn.qna.model.Question;

import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import io.modelcontextprotocol.spec.McpSchema.JsonSchema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseTool implements Function<Map<String,Object>, CallToolResult> {

    private final QuestionService questionService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Tool tool;

    public DatabaseTool(QuestionService questionService) {
        this.questionService = questionService;
        this.tool = new Tool(
            "database_query",
            "Query the question database with various search options. " +
            "Available operations: getAllQuestions, getQuestionById, getQuestionsByCategory, " +
            "getQuestionsByAuthor, searchQuestionsByTitle, searchQuestionsByContent, " +
            "getUnansweredQuestions, getAnsweredQuestions, getQuestionCountByCategory, " +
            "getUnansweredQuestionCount, getTopViewedQuestions",
            createJsonSchema()
        );
    }
    
    private JsonSchema createJsonSchema() {
        // Create JSON schema for the tool parameters
        Map<String, Object> properties = Map.of(
            "operation", Map.of(
                "type", "string",
                "description", "The database operation to perform",
                "enum", List.of(
                    "getAllQuestions", "getQuestionById", "getQuestionsByCategory",
                    "getQuestionsByAuthor", "searchQuestionsByTitle", "searchQuestionsByContent",
                    "getUnansweredQuestions", "getAnsweredQuestions", "getQuestionCountByCategory",
                    "getUnansweredQuestionCount", "getTopViewedQuestions"
                )
            ),
            "parameter", Map.of(
                "type", "string", 
                "description", "Parameter for the operation (id, category, author, keyword, etc.)"
            ),
            "limit", Map.of(
                "type", "integer",
                "description", "Limit for results (used with getTopViewedQuestions)",
                "default", 10
            )
        );

        return new JsonSchema(
            "object",
            properties,
            List.of("operation"), // required fields
            null, null, null
        );
    }
    
    public Tool getTool() {
        return tool;
    }
    
    @Override
    public CallToolResult apply(Map<String, Object> arguments) {
        try {
            String operation = (String) arguments.get("operation");
            String parameter = (String) arguments.get("parameter");
            Integer limit = arguments.containsKey("limit") ? 
                ((Number) arguments.get("limit")).intValue() : 10;
            
            log.info("Executing database operation: {} with parameter: {}", operation, parameter);
            
            Mono<Object> result = executeOperation(operation, parameter, limit);
            Object data = result.block(); // Block for synchronous execution in tool context
            
            String jsonResult = objectMapper.writeValueAsString(data);
            
            return CallToolResult.builder()
                .content(List.of(
                    new TextContent("Database query executed successfully: " + operation + 
                          (parameter != null ? " with parameter: " + parameter : "") +
                          "\nResult: " + jsonResult)
                ))
                .isError(false)
                .build();
                
        } catch (Exception e) {
            log.error("Error executing database operation", e);
            return CallToolResult.builder()
                .content(List.of(
                    new TextContent("Error executing database query: " + e.getMessage())
                ))
                .isError(true)
                .build();
        }
    }
    
    private Mono<Object> executeOperation(String operation, String parameter, Integer limit) {
        return switch (operation) {
            case "getAllQuestions" -> questionService.getAllQuestions().cast(Object.class);
            case "getQuestionById" -> questionService.getQuestionById(parameter).cast(Object.class);
            case "getQuestionsByCategory" -> questionService.getQuestionsByCategory(parameter).cast(Object.class);
            case "getQuestionsByAuthor" -> questionService.getQuestionsByAuthor(parameter).cast(Object.class);
            case "searchQuestionsByTitle" -> questionService.searchQuestionsByTitle(parameter).cast(Object.class);
            case "searchQuestionsByContent" -> questionService.searchQuestionsByContent(parameter).cast(Object.class);
            case "getUnansweredQuestions" -> questionService.getUnansweredQuestions().cast(Object.class);
            case "getAnsweredQuestions" -> questionService.getAnsweredQuestions().cast(Object.class);
            case "getQuestionCountByCategory" -> questionService.getQuestionCountByCategory(parameter).cast(Object.class);
            case "getUnansweredQuestionCount" -> questionService.getUnansweredQuestionCount().cast(Object.class);
            case "getTopViewedQuestions" -> questionService.getTopViewedQuestions(limit).cast(Object.class);
            default -> Mono.error(new IllegalArgumentException("Unknown operation: " + operation));
        };
    }
} 