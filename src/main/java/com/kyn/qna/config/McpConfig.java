package com.kyn.qna.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kyn.qna.tool.DatabaseTool;
import com.kyn.qna.tool.GeminiTool;

import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;

import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.server.transport.WebFluxSseServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema.Implementation;
import io.modelcontextprotocol.spec.McpSchema.LoggingLevel;
import io.modelcontextprotocol.spec.McpSchema.LoggingMessageNotification;
import io.modelcontextprotocol.spec.McpSchema.ServerCapabilities;
import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class McpConfig {
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
    
    @Bean
    @ConditionalOnProperty(prefix = "transport", name = "mode", havingValue = "webflux", matchIfMissing = true)
    public WebFluxSseServerTransportProvider webFluxSseServerTransport(ObjectMapper mapper) {
        String endpoint = "/mcp/message";
        log.info("Creating WebFluxSseServerTransport with endpoint: {}", endpoint);
        return new WebFluxSseServerTransportProvider(mapper, endpoint);
    }

    @Bean
    @ConditionalOnProperty(prefix = "transport", name = "mode", havingValue = "stdio")
    public StdioServerTransportProvider stdioServerTransport() {
        log.info("Creating StdioServerTransport");
        return new StdioServerTransportProvider();
    }
    
    @Bean
    @ConditionalOnProperty(prefix = "transport", name = "mode", havingValue = "webflux", matchIfMissing = true)
    public RouterFunction<?> mcpRouterFunction(WebFluxSseServerTransportProvider transport) {
        log.info("Registering RouterFunction for MCP endpoint");
        return transport.getRouterFunction();
    }
    
    @Bean(destroyMethod = "close")
    public McpAsyncServer mcpAsyncServer(WebFluxSseServerTransportProvider transport, 
                                       DatabaseTool dataTool,
                                       GeminiTool geminiTool) {
        log.info("Initializing McpAsyncServer with transport: {}", transport);
        
        // Create a server with custom configuration
        McpAsyncServer asyncServer = McpServer.async(transport)
            .serverInfo(new Implementation("my-server", "1.0.0"))
            .capabilities(ServerCapabilities.builder()
                .tools(true)         // Enable tool support
                .prompts(false)      // Change to false if not implementing prompts
                .resources(false,false)
                .logging()           // Enable logging support
                .build())
            .build();
        
        // Send initial logging notification
        asyncServer.loggingNotification(LoggingMessageNotification.builder()
            .level(LoggingLevel.INFO)
            .logger("custom-logger")
            .data("Server initialized")
            .build());

        // Register the calculator tool
        var dataToolRegistration = new McpServerFeatures.AsyncToolSpecification(
            dataTool.getTool(),
                                        null
        );

        var geminiToolRegistration = new McpServerFeatures.AsyncToolSpecification(
            geminiTool.getMcpTool(),
            null
        );

        asyncServer.addTool(dataToolRegistration);
        asyncServer.addTool(geminiToolRegistration);

        log.info("MCP Server initialized with capabilities: tools={}, prompts={}, resources={}",
                asyncServer.getServerCapabilities().tools(),
                asyncServer.getServerCapabilities().prompts(),
                asyncServer.getServerCapabilities().resources());
        return asyncServer;
    }
}
