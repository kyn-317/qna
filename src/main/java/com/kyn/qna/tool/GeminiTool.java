package com.kyn.qna.tool;

import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.genai.types.Tool;
import com.google.genai.types.FunctionDeclaration;
import com.google.genai.types.Schema;
import com.google.genai.types.Type;

import com.kyn.qna.service.QuestionService;

import lombok.extern.slf4j.Slf4j;
import java.util.List;
import java.util.Map;

@Configuration
@Slf4j
public class GeminiTool {
    
    private final Tool geminiTool;
    private final DatabaseTool databaseTool;
    private final QuestionService questionService;

    public GeminiTool(QuestionService questionService) {
        this.questionService = questionService;
        this.databaseTool = new DatabaseTool(questionService);
        this.geminiTool = createGeminiToolWithDatabaseFunctions();
    }

    private Tool createGeminiToolWithDatabaseFunctions() {
        // Create function declaration for database query operations
        FunctionDeclaration dbQueryFunction = FunctionDeclaration.builder()
            .name("query_database")
            .description("Query the question database with various operations like searching by category, author, title, content, etc.")
            .parameters(Schema.builder()
                .type(Type.Known.OBJECT)
                .properties(Map.of(
                    "operation", Schema.builder()
                        .type(Type.Known.STRING)
                        .description("The database operation to perform")
                        .enum_(List.of(
                            "getAllQuestions", "getQuestionById", "getQuestionsByCategory",
                            "getQuestionsByAuthor", "searchQuestionsByTitle", "searchQuestionsByContent",
                            "getUnansweredQuestions", "getAnsweredQuestions", "getQuestionCountByCategory",
                            "getUnansweredQuestionCount", "getTopViewedQuestions"
                        ))
                        .build(),
                    "parameter", Schema.builder()
                        .type(Type.Known.STRING)
                        .description("Parameter for the operation (id, category, author, keyword, etc.)")
                        .build(),
                    "limit", Schema.builder()
                        .type(Type.Known.INTEGER)
                        .description("Limit for results (used with getTopViewedQuestions)")
                        .build()
                ))
                .required(List.of("operation"))
                .build())
            .build();

        return Tool.builder()
            .functionDeclarations(List.of(dbQueryFunction))
            .build();
    }

    /**
     * Get the Gemini tool configured with database query capabilities
     */
    public Tool getGeminiTool() {
        return geminiTool;
    }

    /**
     * Get the MCP DatabaseTool for registration with MCP server
     */
    public DatabaseTool getDatabaseTool() {
        return databaseTool;
    }

    /**
     * Get the MCP tool definition for registration
     */
    public io.modelcontextprotocol.spec.McpSchema.Tool getMcpTool() {
        return databaseTool.getTool();
    }

    /**
     * Execute database query operation through the database tool
     * This method can be called by Gemini function calling
     */
    public String executeQuery(Map<String, Object> arguments) {
        try {
            log.info("Executing database query with arguments: {}", arguments);
            
            var result = databaseTool.apply(arguments);
            
            if (result.isError()) {
                return "Error: " + result.content().get(0);
            } else {
                return result.content().get(0).toString();
            }
        } catch (Exception e) {
            log.error("Error executing database query", e);
            return "Error executing database query: " + e.getMessage();
        }
    }
}

