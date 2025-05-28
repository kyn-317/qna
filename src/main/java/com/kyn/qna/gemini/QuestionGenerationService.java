package com.kyn.qna.gemini;

import com.kyn.qna.mcp.McpContextService;
import com.kyn.qna.model.Question;
import com.kyn.qna.repository.QuestionRepository;
import com.kyn.qna.repository.QASummaryRepository;
import com.kyn.qna.service.HashService;
import com.google.common.collect.ImmutableList;
import com.google.genai.Client;
import com.google.genai.ResponseStream;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import io.modelcontextprotocol.core.Fact;
import io.modelcontextprotocol.core.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
// import org.springframework.beans.factory.annotation.Value; // No longer needed for API key here
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
// import java.util.Optional; // No longer directly used here, but good to keep if needed in future
import java.util.stream.Collectors;

@Service
public class QuestionGenerationService {

    private static final Logger logger = LoggerFactory.getLogger(QuestionGenerationService.class);
    private final McpContextService mcpContextService;
    private final Client geminiClient; // Injected directly
    private final String modelName = "gemini-1.5-flash-latest";
    private final QuestionRepository questionRepository;
    private final QASummaryRepository qaSummaryRepository;
    private final HashService hashService;

    // Constructor updated to accept Client
    public QuestionGenerationService(McpContextService mcpContextService,
                                     Client geminiClient, // Accept Client directly
                                     QuestionRepository questionRepository,
                                     QASummaryRepository qaSummaryRepository,
                                     HashService hashService) {
        this.mcpContextService = mcpContextService;
        this.geminiClient = geminiClient; // Assign injected client
        this.questionRepository = questionRepository;
        this.qaSummaryRepository = qaSummaryRepository;
        this.hashService = hashService;
    }

    public Question generateQuestion(String sessionId) {
        logger.info("Starting question generation for session ID: {}", sessionId);

        io.modelcontextprotocol.core.Context context = mcpContextService.getContext(sessionId);
        if (context == null) {
            logger.error("MCP Context not found for session ID: {}. Cannot generate question.", sessionId);
            throw new IllegalStateException("MCP Context not found for session ID: " + sessionId);
        }

        String technologyStack = extractFactValue(context, "technologyStack");
        String experienceLevel = extractFactValue(context, "experienceLevel");

        if (technologyStack == null || experienceLevel == null) {
            logger.error("Essential facts (technologyStack or experienceLevel) missing in context for session ID: {}", sessionId);
            throw new IllegalStateException("Essential facts missing in context for session ID: " + sessionId);
        }

        List<String> previousQuestionTexts = context.getMessages().stream()
                .filter(msg -> "assistant".equalsIgnoreCase(msg.getRole()))
                .map(Message::getContent)
                .collect(Collectors.toList());

        String prompt = buildPrompt(technologyStack, experienceLevel, previousQuestionTexts);
        logger.debug("Generated prompt for Gemini: {}", prompt);

        try {
            GenerateContentConfig config = GenerateContentConfig.builder()
                    .responseMimeType("text/plain")
                    .build();

            List<Content> contents = ImmutableList.of(
                    Content.builder()
                            .role("user")
                            .parts(ImmutableList.of(Part.fromText(prompt)))
                            .build()
            );
            // Use the injected geminiClient
            ResponseStream<GenerateContentResponse> responseStream = geminiClient.getModels().generateContentStream(modelName, contents, config);
            StringBuilder generatedTextBuilder = new StringBuilder();
            for (GenerateContentResponse res : responseStream) {
                if (res.candidates().isEmpty() || res.candidates().get().get(0).content().isEmpty() || res.candidates().get().get(0).content().get().parts().isEmpty()) {
                    continue;
                }
                List<Part> parts = res.candidates().get().get(0).content().get().parts().get();
                for (Part part : parts) {
                    if (part.text() != null) {
                        generatedTextBuilder.append(part.text());
                    }
                }
            }
            responseStream.close();

            String generatedQuestionText = generatedTextBuilder.toString().trim();

            if (generatedQuestionText.isEmpty()) {
                logger.error("Gemini API returned an empty response for session ID: {}", sessionId);
                throw new RuntimeException("Gemini API returned an empty response.");
            }

            String potentialHash = hashService.generateSha256Hash(generatedQuestionText);
            boolean exists = qaSummaryRepository.existsByQuestionTextHashAndTechnologyStackAndExperienceLevel(
                    potentialHash, technologyStack, experienceLevel);

            if (exists) {
                logger.warn("Generated question for session ID {} (Hash: {}) is a potential duplicate for Technology: {}, Experience: {}. Proceeding with the question.",
                        sessionId, potentialHash, technologyStack, experienceLevel);
            }

            Question newQuestion = new Question();
            newQuestion.setText(generatedQuestionText);
            newQuestion.setTechnologyStack(technologyStack);
            newQuestion.setExperienceLevel(experienceLevel);
            newQuestion.setGeneratedBy(modelName);
            newQuestion.setCreatedAt(LocalDateTime.now());

            Question savedQuestion = questionRepository.save(newQuestion);
            logger.info("Successfully generated and saved question ID {} for session ID {}: {}", savedQuestion.getId(), sessionId, savedQuestion.getText());
            return savedQuestion;

        } catch (Exception e) {
            logger.error("Error calling Gemini API or processing response for session ID: {}", sessionId, e);
            throw new RuntimeException("Error generating question via Gemini API: " + e.getMessage(), e);
        }
    }

    private String extractFactValue(io.modelcontextprotocol.core.Context context, String factKey) {
        if (context.getFacts() == null) {
            return null;
        }
        return context.getFacts().stream()
                .filter(fact -> factKey.equals(fact.getKey()))
                .map(Fact::getValue)
                .findFirst()
                .orElse(null);
    }

    private String buildPrompt(String technologyStack, String experienceLevel, List<String> previousQuestionTexts) {
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("You are an interviewer. Generate one technical interview question appropriate for a candidate with ")
                .append(experienceLevel).append(" of experience in ").append(technologyStack)
                .append(". Focus on core concepts and practical application.");

        if (previousQuestionTexts != null && !previousQuestionTexts.isEmpty()) {
            promptBuilder.append(" Avoid asking questions similar to the following topics/questions already covered: ");
            String previousQuestionsString = String.join("; ", previousQuestionTexts);
            promptBuilder.append(previousQuestionsString).append(".");
        }
        promptBuilder.append(" The question should be clear and concise. Do not provide an answer, only the question.");
        return promptBuilder.toString();
    }
}
