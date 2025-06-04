package com.kyn.qna.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kyn.qna.dto.AdditionalQuestion;
import com.kyn.qna.dto.QuestionRequest;
import com.kyn.qna.dto.SimplifiedQuestionRequest;
import com.kyn.qna.entity.Question;
import com.kyn.qna.entity.SimplifiedQuestion;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class QuestionManageServiceTest {
    
    @Mock
    private QuestionService questionService;
    
    @Mock
    private GeminiService geminiService;
    
    private QuestionManageService questionManageService;
    private ObjectMapper objectMapper;
    
    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        questionManageService = new QuestionManageService(questionService, geminiService, objectMapper);
    }
    
    @Test
    void testCreateQuestion_Success() {
        // Given
        QuestionRequest request = QuestionRequest.builder()
            .category("Java")
            .expYears(3)
            .build();
            
        SimplifiedQuestion historyQuestion = SimplifiedQuestion.builder()
            ._id("history-1")
            .question("Java 기본 개념은?")
            .category("Java")
            .expYears(3)
            .simplifiedDetail("자바 기본 개념에 대한 질문")
            .build();
            
        String geminiResponse = """
            {
                "question": "Java의 다형성(Polymorphism)에 대해 설명해주세요."
            }
            """;
            
        Question savedQuestion = Question.builder()
            ._id("new-question-id")
            .question("Java의 다형성(Polymorphism)에 대해 설명해주세요.")
            .category("Java")
            .expYears(3)
            .createdAt(LocalDateTime.now())
            .build();
        
        when(questionService.getSimplifiedQuestionByCategory("Java"))
            .thenReturn(Flux.just(historyQuestion));
        when(geminiService.generateResponse(anyString()))
            .thenReturn(Mono.just(geminiResponse));
        when(questionService.insert(any(QuestionRequest.class)))
            .thenReturn(Mono.just(savedQuestion));
        
        // When & Then
        StepVerifier.create(questionManageService.createQuestion(request))
            .assertNext(question -> {
                assertNotNull(question);
                assertEquals("new-question-id", question.get_id());
                assertEquals("Java의 다형성(Polymorphism)에 대해 설명해주세요.", question.getQuestion());
                assertEquals("Java", question.getCategory());
                assertEquals(3, question.getExpYears());
            })
            .verifyComplete();
            
        verify(questionService).getSimplifiedQuestionByCategory("Java");
        verify(geminiService).generateResponse(contains("Java"));
        verify(questionService).insert(any(QuestionRequest.class));
    }
    
    @Test
    void testUserAnswered_Success() {
        // Given
        QuestionRequest request = QuestionRequest.builder()
            ._id("question-id")
            .question("Java의 다형성에 대해 설명해주세요.")
            .userAnswer("다형성은 하나의 인터페이스로 여러 타입을 처리할 수 있는 능력입니다.")
            .category("Java")
            .expYears(3)
            .build();
            
        String geminiGradingResponse = """
            {
                "score": 85,
                "modelAnswer": "다형성은 객체지향 프로그래밍의 핵심 개념으로, 하나의 인터페이스나 기본 클래스 타입으로 여러 구현체를 다룰 수 있는 능력을 의미합니다. 예를 들어 Animal 타입 변수로 Dog, Cat 객체를 모두 참조할 수 있습니다."
            }
            """;
            
        Question updatedQuestion = Question.builder()
            ._id("question-id")
            .question("Java의 다형성에 대해 설명해주세요.")
            .userAnswer("다형성은 하나의 인터페이스로 여러 타입을 처리할 수 있는 능력입니다.")
            .modelAnswer("다형성은 객체지향 프로그래밍의 핵심 개념으로, 하나의 인터페이스나 기본 클래스 타입으로 여러 구현체를 다룰 수 있는 능력을 의미합니다. 예를 들어 Animal 타입 변수로 Dog, Cat 객체를 모두 참조할 수 있습니다.")
            .score(85)
            .category("Java")
            .expYears(3)
            .updatedAt(LocalDateTime.now())
            .build();
            
        SimplifiedQuestion simplifiedQuestion = SimplifiedQuestion.builder()
            ._id("simplified-id")
            .question("Java의 다형성에 대해 설명해주세요.")
            .simplifiedDetail("다형성: 하나의 인터페이스로 여러 타입 처리")
            .category("Java")
            .expYears(3)
            .build();
            
        String simplifiedResponse = """
            {
                "_id": "simplified-id",
                "question": "Java의 다형성에 대해 설명해주세요.",
                "simplifiedDetail": "다형성: 하나의 인터페이스로 여러 타입 처리",
                "category": "Java",
                "expYears": "3"
            }
            """;
        
        when(geminiService.generateResponse(contains("grade")))
            .thenReturn(Mono.just(geminiGradingResponse));
        when(questionService.update(any(QuestionRequest.class)))
            .thenReturn(Mono.just(updatedQuestion));
        when(geminiService.generateResponse(contains("simplify")))
            .thenReturn(Mono.just(simplifiedResponse));
        when(questionService.saveSimplifiedQuestion(any(SimplifiedQuestionRequest.class)))
            .thenReturn(Mono.just(simplifiedQuestion));
        
        // When & Then
        StepVerifier.create(questionManageService.userAnswered(request))
            .assertNext(question -> {
                assertNotNull(question);
                assertEquals("question-id", question.get_id());
                assertEquals(85, question.getScore());
                assertNotNull(question.getModelAnswer());
                assertTrue(question.getModelAnswer().contains("다형성"));
            })
            .verifyComplete();
            
        verify(geminiService).generateResponse(contains("grade"));
        verify(questionService).update(any(QuestionRequest.class));
        // SimplifiedQuestion creation은 비동기이므로 직접 verify하기 어려움
    }
    
    @Test
    void testGetAdditionalQuestionsAndAnswers_Success() {
        // Given
        QuestionRequest request = QuestionRequest.builder()
            ._id("question-id")
            .question("Java의 다형성에 대해 설명해주세요.")
            .userAnswer("다형성은 하나의 인터페이스로 여러 타입을 처리할 수 있는 능력입니다.")
            .category("Java")
            .expYears(3)
            .build();
            
        String additionalQuestionsResponse = """
            [
                {
                    "question": "인터페이스와 추상클래스의 차이점은?",
                    "answer": "인터페이스는 다중 상속이 가능하고 모든 메서드가 추상메서드(default 제외)인 반면, 추상클래스는 단일 상속만 가능하고 구현된 메서드를 가질 수 있습니다."
                },
                {
                    "question": "오버라이딩과 오버로딩의 차이는?",
                    "answer": "오버라이딩은 상속받은 메서드를 재정의하는 것이고, 오버로딩은 같은 이름의 메서드를 매개변수를 달리해서 여러 개 정의하는 것입니다."
                }
            ]
            """;
            
        List<AdditionalQuestion> expectedAdditionalQuestions = Arrays.asList(
            AdditionalQuestion.builder()
                .question("인터페이스와 추상클래스의 차이점은?")
                .answer("인터페이스는 다중 상속이 가능하고 모든 메서드가 추상메서드(default 제외)인 반면, 추상클래스는 단일 상속만 가능하고 구현된 메서드를 가질 수 있습니다.")
                .build(),
            AdditionalQuestion.builder()
                .question("오버라이딩과 오버로딩의 차이는?")
                .answer("오버라이딩은 상속받은 메서드를 재정의하는 것이고, 오버로딩은 같은 이름의 메서드를 매개변수를 달리해서 여러 개 정의하는 것입니다.")
                .build()
        );
            
        Question updatedQuestion = Question.builder()
            ._id("question-id")
            .question("Java의 다형성에 대해 설명해주세요.")
            .userAnswer("다형성은 하나의 인터페이스로 여러 타입을 처리할 수 있는 능력입니다.")
            .additionalQuestions(expectedAdditionalQuestions)
            .category("Java")
            .expYears(3)
            .updatedAt(LocalDateTime.now())
            .build();
            
        SimplifiedQuestion simplifiedQuestion = SimplifiedQuestion.builder()
            ._id("simplified-id")
            .question("Java의 다형성에 대해 설명해주세요.")
            .simplifiedDetail("다형성과 관련 개념들에 대한 Q&A")
            .category("Java")
            .expYears(3)
            .build();
            
        String simplifiedResponse = """
            {
                "_id": "simplified-id",
                "question": "Java의 다형성에 대해 설명해주세요.",
                "simplifiedDetail": "다형성과 관련 개념들에 대한 Q&A",
                "category": "Java",
                "expYears": "3"
            }
            """;
        
        when(geminiService.generateResponse(contains("records")))
            .thenReturn(Mono.just(additionalQuestionsResponse));
        when(questionService.update(any(QuestionRequest.class)))
            .thenReturn(Mono.just(updatedQuestion));
        when(geminiService.generateResponse(contains("simplify")))
            .thenReturn(Mono.just(simplifiedResponse));
        when(questionService.saveSimplifiedQuestion(any(SimplifiedQuestionRequest.class)))
            .thenReturn(Mono.just(simplifiedQuestion));
        
        // When & Then
        StepVerifier.create(questionManageService.getAdditionalQuestionsAndAnswers(request))
            .assertNext(question -> {
                assertNotNull(question);
                assertEquals("question-id", question.get_id());
                assertNotNull(question.getAdditionalQuestions());
                assertEquals(2, question.getAdditionalQuestions().size());
                
                assertEquals("인터페이스와 추상클래스의 차이점은?", 
                    question.getAdditionalQuestions().get(0).getQuestion());
                assertEquals("오버라이딩과 오버로딩의 차이는?", 
                    question.getAdditionalQuestions().get(1).getQuestion());
            })
            .verifyComplete();
            
        verify(geminiService).generateResponse(contains("records"));
        verify(questionService).update(any(QuestionRequest.class));
    }
    
    @Test
    void testCreateQuestion_EmptyHistory() {
        // Given
        QuestionRequest request = QuestionRequest.builder()
            .category("React")
            .expYears(2)
            .build();
            
        String geminiResponse = """
            {
                "question": "React의 useState Hook에 대해 설명해주세요."
            }
            """;
            
        Question savedQuestion = Question.builder()
            ._id("new-question-id")
            .question("React의 useState Hook에 대해 설명해주세요.")
            .category("React")
            .expYears(2)
            .createdAt(LocalDateTime.now())
            .build();
        
        when(questionService.getSimplifiedQuestionByCategory("React"))
            .thenReturn(Flux.empty()); // 히스토리가 없는 경우
        when(geminiService.generateResponse(anyString()))
            .thenReturn(Mono.just(geminiResponse));
        when(questionService.insert(any(QuestionRequest.class)))
            .thenReturn(Mono.just(savedQuestion));
        
        // When & Then
        StepVerifier.create(questionManageService.createQuestion(request))
            .assertNext(question -> {
                assertNotNull(question);
                assertEquals("React의 useState Hook에 대해 설명해주세요.", question.getQuestion());
                assertEquals("React", question.getCategory());
            })
            .verifyComplete();
    }
    
    @Test
    void testUserAnswered_GeminiError() {
        // Given
        QuestionRequest request = QuestionRequest.builder()
            ._id("question-id")
            .question("Java의 다형성에 대해 설명해주세요.")
            .userAnswer("잘 모르겠습니다.")
            .category("Java")
            .expYears(3)
            .build();
        
        when(geminiService.generateResponse(anyString()))
            .thenReturn(Mono.error(new RuntimeException("Gemini API Error")));
        
        // When & Then
        StepVerifier.create(questionManageService.userAnswered(request))
            .expectError(RuntimeException.class)
            .verify();
            
        verify(geminiService).generateResponse(anyString());
        verifyNoInteractions(questionService);
    }
    
    @Test
    void testGetAdditionalQuestionsAndAnswers_MalformedJson() {
        // Given
        QuestionRequest request = QuestionRequest.builder()
            ._id("question-id")
            .question("Java의 다형성에 대해 설명해주세요.")
            .category("Java")
            .expYears(3)
            .build();
            
        String malformedJsonResponse = "This is not valid JSON!";
            
        Question updatedQuestion = Question.builder()
            ._id("question-id")
            .question("Java의 다형성에 대해 설명해주세요.")
            .additionalQuestions(List.of()) // 빈 리스트로 fallback
            .category("Java")
            .expYears(3)
            .build();
        
        when(geminiService.generateResponse(anyString()))
            .thenReturn(Mono.just(malformedJsonResponse));
        when(questionService.update(any(QuestionRequest.class)))
            .thenReturn(Mono.just(updatedQuestion));
        
        // When & Then
        StepVerifier.create(questionManageService.getAdditionalQuestionsAndAnswers(request))
            .assertNext(question -> {
                assertNotNull(question);
                assertEquals("question-id", question.get_id());
                assertNotNull(question.getAdditionalQuestions());
                assertTrue(question.getAdditionalQuestions().isEmpty());
            })
            .verifyComplete();
    }
} 