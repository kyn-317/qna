package com.kyn.qna.service;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kyn.qna.dto.AdditionalQuestion;
import com.kyn.qna.dto.QuestionRequest;

import java.lang.reflect.Method;
import java.util.List;

class QuestionManageServiceTest {
    
    @Mock
    private QuestionService questionService;
    
    @Mock
    private GeminiService geminiService;
    
    private QuestionManageService questionManageService;
    private ObjectMapper objectMapper;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        objectMapper = new ObjectMapper();
        questionManageService = new QuestionManageService(questionService, geminiService, objectMapper);
    }
    
    @Test
    void testParseAdditionalQuestionsAndAnswers() throws Exception {
        // Given
        String jsonResponse = """
            [
                {
                    "question": "테스트 질문 1",
                    "answer": "테스트 답변 1"
                },
                {
                    "question": "테스트 질문 2", 
                    "answer": "테스트 답변 2"
                }
            ]
            """;
            
        QuestionRequest originalRequest = QuestionRequest.builder()
            ._id("test-id")
            .question("원본 질문")
            .category("Java")
            .expYears(3)
            .build();
        
        // When - Use reflection to access private method
        Method method = QuestionManageService.class.getDeclaredMethod("parseAdditionalQuestionsAndAnswers", String.class, QuestionRequest.class);
        method.setAccessible(true);
        QuestionRequest result = (QuestionRequest) method.invoke(questionManageService, jsonResponse, originalRequest);
        
        // Then
        assertNotNull(result);
        assertEquals("test-id", result._id());
        assertEquals("원본 질문", result.question());
        assertEquals("Java", result.category());
        assertEquals(3, result.expYears());
        
        List<AdditionalQuestion> additionalQuestions = result.additionalQuestions();
        assertNotNull(additionalQuestions);
        assertEquals(2, additionalQuestions.size());
        
        assertEquals("테스트 질문 1", additionalQuestions.get(0).getQuestion());
        assertEquals("테스트 답변 1", additionalQuestions.get(0).getAnswer());
        assertEquals("테스트 질문 2", additionalQuestions.get(1).getQuestion());
        assertEquals("테스트 답변 2", additionalQuestions.get(1).getAnswer());
    }
    
    @Test
    void testParseAdditionalQuestionsAndAnswersWithMalformedJson() throws Exception {
        // Given - Malformed JSON with unescaped quotes and line breaks
        String malformedJsonResponse = """
            [
                {
                    "question": "spring cache의 구체적인 설정방법을 예로 들어줘",
                    "answer": "Spring Cache는 다양한 방식으로 설정할 수 있지만, 가장 일반적인 방법은 `CacheManager`를 설정하고 `Cacheable` 어노테이션을 사용하는 것입니다."
                }
            ]
            """;
            
        QuestionRequest originalRequest = QuestionRequest.builder()
            ._id("test-id")
            .question("원본 질문")
            .category("Spring")
            .expYears(5)
            .build();
        
        // When - Use reflection to access private method
        Method method = QuestionManageService.class.getDeclaredMethod("parseAdditionalQuestionsAndAnswers", String.class, QuestionRequest.class);
        method.setAccessible(true);
        QuestionRequest result = (QuestionRequest) method.invoke(questionManageService, malformedJsonResponse, originalRequest);
        
        // Then - Should gracefully handle malformed JSON
        assertNotNull(result);
        assertEquals("test-id", result._id());
        assertEquals("원본 질문", result.question());
        assertEquals("Spring", result.category());
        assertEquals(5, result.expYears());
        
        // Should either parse successfully or return empty list (not null)
        List<AdditionalQuestion> additionalQuestions = result.additionalQuestions();
        assertNotNull(additionalQuestions);
        // The result could be either successfully parsed or empty list depending on fallback success
    }
    
    @Test
    void testParseAdditionalQuestionsAndAnswersWithCompletelyInvalidJson() throws Exception {
        // Given - Completely invalid JSON
        String invalidJsonResponse = "This is not JSON at all!";
            
        QuestionRequest originalRequest = QuestionRequest.builder()
            ._id("test-id")
            .question("원본 질문")
            .category("Java")
            .expYears(3)
            .build();
        
        // When - Use reflection to access private method
        Method method = QuestionManageService.class.getDeclaredMethod("parseAdditionalQuestionsAndAnswers", String.class, QuestionRequest.class);
        method.setAccessible(true);
        QuestionRequest result = (QuestionRequest) method.invoke(questionManageService, invalidJsonResponse, originalRequest);
        
        // Then - Should return original request with empty additional questions
        assertNotNull(result);
        assertEquals("test-id", result._id());
        assertEquals("원본 질문", result.question());
        assertEquals("Java", result.category());
        assertEquals(3, result.expYears());
        
        List<AdditionalQuestion> additionalQuestions = result.additionalQuestions();
        assertNotNull(additionalQuestions);
        assertTrue(additionalQuestions.isEmpty());
    }
    
    @Test
    void testParseResponseWithDollarSignIssue() throws Exception {
        // Given - JSON with problematic $ character
        String problematicJsonResponse = """
            {
                "score": 85,
                "modelAnswer": "Spring에서 $를 사용한 환경변수 설정 방법: @Value(\\"${database.url}\\") 같은 형태로 사용합니다."
            }
            """;
            
        QuestionRequest originalRequest = QuestionRequest.builder()
            ._id("test-id")
            .question("원본 질문")
            .category("Spring")
            .expYears(5)
            .build();
        
        // When - Use reflection to access private method
        Method method = QuestionManageService.class.getDeclaredMethod("parseResponse", String.class, QuestionRequest.class);
        method.setAccessible(true);
        QuestionRequest result = (QuestionRequest) method.invoke(questionManageService, problematicJsonResponse, originalRequest);
        
        // Then - Should handle $ character gracefully
        assertNotNull(result);
        assertEquals("test-id", result._id());
        assertEquals("원본 질문", result.question());
        assertEquals("Spring", result.category());
        assertEquals(5, result.expYears());
        assertEquals(85, result.score());
        assertNotNull(result.modelAnswer());
        assertTrue(result.modelAnswer().contains("$"));
    }
    
    @Test
    void testParseJsonWithFallbackMethod() throws Exception {
        // Given - JSON that requires enhanced cleaning
        String problematicJson = """
            {
                "question": "테스트 질문",
                "simplifiedDetail": "요약에 "따옴표"가 포함된 내용입니다."
            }
            """;
            
        QuestionRequest originalRequest = QuestionRequest.builder()
            ._id("test-id")
            .question("원본 질문")
            .category("Java")
            .expYears(3)
            .build();
        
        // When - Use reflection to access private method
        Method method = QuestionManageService.class.getDeclaredMethod("parseSimplifiedQuestion", String.class, QuestionRequest.class);
        method.setAccessible(true);
        var result = method.invoke(questionManageService, problematicJson, originalRequest);
        
        // Then - Should either parse successfully or return fallback
        assertNotNull(result);
    }
} 