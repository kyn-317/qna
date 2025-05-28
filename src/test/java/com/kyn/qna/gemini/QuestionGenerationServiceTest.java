package com.kyn.qna.gemini;

import com.kyn.qna.mcp.McpContextService;
import com.kyn.qna.model.Question;
import com.kyn.qna.repository.QuestionRepository;
import com.kyn.qna.repository.QASummaryRepository;
import com.kyn.qna.service.HashService;
import com.google.genai.Client;
import com.google.genai.Models;
import com.google.genai.ResponseStream;
import com.google.genai.types.Candidate;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import io.modelcontextprotocol.core.Fact;
import io.modelcontextprotocol.core.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuestionGenerationServiceTest {

    @Mock
    private McpContextService mcpContextService;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private QASummaryRepository qaSummaryRepository;

    @Mock
    private HashService hashService;

    @Mock
    private Client googleGenaiClient;

    @Mock
    private Models modelsApi;

    @Mock
    private ResponseStream<GenerateContentResponse> responseStream;

    @Mock
    private GenerateContentResponse generateContentResponse;

    @Mock
    private Candidate candidate;

    @Mock
    private Content responseContent;

    @Mock
    private Part responsePart;

    @InjectMocks
    private QuestionGenerationService questionGenerationService;

    private io.modelcontextprotocol.core.Context mockContext;
    private final String sessionId = "test-session-id";
    private final String technologyStack = "Java";
    private final String experienceLevel = "Mid-Level";
    private final String generatedQuestionText = "Explain Spring Boot auto-configuration.";
    private final String questionHash = "someHashValue";

    @BeforeEach
    void setUp() {
        mockContext = new io.modelcontextprotocol.core.Context();
        List<Fact> facts = new ArrayList<>();
        facts.add(new Fact("technologyStack", technologyStack));
        facts.add(new Fact("experienceLevel", experienceLevel));
        mockContext.setFacts(facts);

        List<Message> messages = new ArrayList<>();
        Message prevQuestion = new Message();
        prevQuestion.setRole("assistant");
        prevQuestion.setContent("Previous question 1");
        messages.add(prevQuestion);
        mockContext.setMessages(messages);

        // Common mocking for googleGenaiClient.getModels()
        lenient().when(googleGenaiClient.getModels()).thenReturn(modelsApi);
    }

    private void setupSuccessMocks() {
        when(mcpContextService.getContext(sessionId)).thenReturn(mockContext);

        // Mocking the chain for Gemini response
        when(modelsApi.generateContentStream(anyString(), any(List.class), any())).thenReturn(responseStream);
        // Simulate the iterable nature of ResponseStream
        when(responseStream.iterator()).thenReturn(Collections.singletonList(generateContentResponse).iterator());
        when(generateContentResponse.candidates()).thenReturn(Optional.of(Collections.singletonList(candidate)));
        when(candidate.content()).thenReturn(Optional.of(responseContent));
        when(responseContent.parts()).thenReturn(Optional.of(Collections.singletonList(responsePart)));
        when(responsePart.text()).thenReturn(generatedQuestionText);


        when(hashService.generateSha256Hash(generatedQuestionText)).thenReturn(questionHash);
        when(qaSummaryRepository.existsByQuestionTextHashAndTechnologyStackAndExperienceLevel(questionHash, technologyStack, experienceLevel)).thenReturn(false);

        when(questionRepository.save(any(Question.class))).thenAnswer(invocation -> {
            Question q = invocation.getArgument(0);
            q.setId("test-question-id"); // Simulate saving and getting an ID
            return q;
        });
    }

    @Test
    void testGenerateQuestion_Success() {
        setupSuccessMocks();

        Question result = questionGenerationService.generateQuestion(sessionId);

        assertNotNull(result);
        assertEquals(generatedQuestionText, result.getText());
        assertEquals(technologyStack, result.getTechnologyStack());
        assertEquals(experienceLevel, result.getExperienceLevel());
        assertEquals("gemini-1.5-flash-latest", result.getGeneratedBy());
        assertNotNull(result.getCreatedAt());
        assertEquals("test-question-id", result.getId());

        verify(mcpContextService).getContext(sessionId);

        ArgumentCaptor<List<Content>> contentCaptor = ArgumentCaptor.forClass(List.class);
        verify(modelsApi).generateContentStream(eq("gemini-1.5-flash-latest"), contentCaptor.capture(), any());
        // Verify prompt construction (simplified check on the captured content)
        assertFalse(contentCaptor.getValue().isEmpty());
        String promptSent = contentCaptor.getValue().get(0).getParts().get(0).getText();
        assertTrue(promptSent.contains(technologyStack));
        assertTrue(promptSent.contains(experienceLevel));
        assertTrue(promptSent.contains("Previous question 1"));


        verify(hashService).generateSha256Hash(generatedQuestionText);
        verify(qaSummaryRepository).existsByQuestionTextHashAndTechnologyStackAndExperienceLevel(questionHash, technologyStack, experienceLevel);
        verify(questionRepository).save(any(Question.class));
    }

    @Test
    void testGenerateQuestion_ContextNotFound() {
        when(mcpContextService.getContext(sessionId)).thenReturn(null);

        assertThrows(IllegalStateException.class, () -> questionGenerationService.generateQuestion(sessionId));

        verify(mcpContextService).getContext(sessionId);
        verifyNoInteractions(googleGenaiClient, questionRepository, qaSummaryRepository, hashService);
    }

    @Test
    void testGenerateQuestion_MissingFactsInContext() {
        mockContext.setFacts(Collections.singletonList(new Fact("experienceLevel", experienceLevel))); // Missing tech stack
        when(mcpContextService.getContext(sessionId)).thenReturn(mockContext);

        assertThrows(IllegalStateException.class, () -> questionGenerationService.generateQuestion(sessionId));

        verify(mcpContextService).getContext(sessionId);
        verifyNoInteractions(googleGenaiClient, questionRepository, qaSummaryRepository, hashService);
    }

    @Test
    void testGenerateQuestion_GeminiApiError() {
        when(mcpContextService.getContext(sessionId)).thenReturn(mockContext);
        when(modelsApi.generateContentStream(anyString(), any(List.class), any())).thenThrow(new RuntimeException("Gemini API down"));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> questionGenerationService.generateQuestion(sessionId));
        assertTrue(exception.getMessage().contains("Error generating question via Gemini API"));

        verify(mcpContextService).getContext(sessionId);
        verify(modelsApi).generateContentStream(anyString(), any(List.class), any());
        verifyNoInteractions(questionRepository, qaSummaryRepository, hashService); // hashService might be called if error is after hash
    }
    
    @Test
    void testGenerateQuestion_GeminiReturnsEmptyResponse() {
        when(mcpContextService.getContext(sessionId)).thenReturn(mockContext);
        when(modelsApi.generateContentStream(anyString(), any(List.class), any())).thenReturn(responseStream);
        when(responseStream.iterator()).thenReturn(Collections.singletonList(generateContentResponse).iterator());
        when(generateContentResponse.candidates()).thenReturn(Optional.of(Collections.singletonList(candidate)));
        when(candidate.content()).thenReturn(Optional.of(responseContent));
        when(responseContent.parts()).thenReturn(Optional.of(Collections.singletonList(responsePart)));
        when(responsePart.text()).thenReturn(""); // Empty text

        RuntimeException exception = assertThrows(RuntimeException.class, () -> questionGenerationService.generateQuestion(sessionId));
        assertEquals("Gemini API returned an empty response.", exception.getMessage());

        verify(mcpContextService).getContext(sessionId);
        verify(modelsApi).generateContentStream(anyString(), any(List.class), any());
        verifyNoInteractions(questionRepository, qaSummaryRepository, hashService);
    }


    @Test
    void testGenerateQuestion_DuplicateFound() {
        setupSuccessMocks(); // Sets up mocks for a successful flow initially
        when(qaSummaryRepository.existsByQuestionTextHashAndTechnologyStackAndExperienceLevel(questionHash, technologyStack, experienceLevel)).thenReturn(true); // Override for duplicate

        Question result = questionGenerationService.generateQuestion(sessionId);

        assertNotNull(result); // Question should still be generated and saved
        assertEquals(generatedQuestionText, result.getText());

        verify(mcpContextService).getContext(sessionId);
        verify(modelsApi).generateContentStream(anyString(), any(List.class), any());
        verify(hashService).generateSha256Hash(generatedQuestionText);
        verify(qaSummaryRepository).existsByQuestionTextHashAndTechnologyStackAndExperienceLevel(questionHash, technologyStack, experienceLevel);
        verify(questionRepository).save(any(Question.class)); // Ensure save is still called
    }
}
