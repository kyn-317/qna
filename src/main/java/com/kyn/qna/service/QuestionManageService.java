package com.kyn.qna.service;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kyn.qna.dto.AdditionalQuestion;
import com.kyn.qna.dto.QuestionRequest;
import com.kyn.qna.dto.SimplifiedQuestionRequest;
import com.kyn.qna.entity.Question;
import com.kyn.qna.entity.SimplifiedQuestion;
import com.kyn.qna.mapper.EntityDtoMapper;
import com.kyn.qna.util.JsonStringUtil;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class QuestionManageService {
    private final QuestionService questionService;
    private final GeminiService geminiService;
    private final ObjectMapper objectMapper;

    public QuestionManageService(QuestionService questionService, GeminiService geminiService, ObjectMapper objectMapper) {
        this.questionService = questionService;
        this.geminiService = geminiService;
        this.objectMapper = objectMapper;
    }

    public Mono<Question> createQuestion(QuestionRequest questionRequest){

        //get history questions by category
        return questionService.getSimplifiedQuestionByCategory(questionRequest.category())
            .collectList()
            //create prompt
            .map(questions -> 
                questionCreationPrompt
                .replace("{history}", questions.toString())
                .replace("{category}", questionRequest.category())
                .replace("{expYears}", String.valueOf(questionRequest.expYears())))    
            //generate response
            .flatMap(geminiService::generateResponse)
            .map(JsonStringUtil::extractJsonFromResponse)
            //save question
            .map(jsonResponse -> parseResponse(jsonResponse, questionRequest))
            .flatMap(questionService::insert);
    }

    public Mono<Question> userAnswered(QuestionRequest questionRequest){
        
        return geminiService.generateResponse(questionGradingPrompt
            .replace("{question}", questionRequest.toString()))
            .map(JsonStringUtil::extractJsonFromResponse)
            .map(jsonResponse -> parseResponse(jsonResponse, questionRequest))
            .flatMap(questionService::update)
            .doOnSuccess(question -> saveSimplifiedQuestion(question));
    }

    public Mono<Question> getAdditionalQuestionsAndAnswers(QuestionRequest questionRequest){
        return geminiService.generateResponse(
            questionAdditionalQuestion
            .replace("{records}", questionRequest.toString())
        )
        .map(JsonStringUtil::extractJsonFromResponse)
        .map(jsonResponse -> parseAdditionalQuestionsAndAnswers(jsonResponse, questionRequest))
        .flatMap(questionService::update)
        .doOnSuccess(question -> saveSimplifiedQuestion(question));
        
    }


    private Mono<SimplifiedQuestion> saveSimplifiedQuestion(QuestionRequest question){
        
        return geminiService.generateResponse(questionSimplifiedQuestion
                .replace("{question}", question.toString()))
            .map(JsonStringUtil::extractJsonFromResponse)
            .map(jsonResponse -> parseSimplifiedQuestion(jsonResponse, question))
            .flatMap(questionService::saveSimplifiedQuestion);
    }
    
    private Mono<SimplifiedQuestion> saveSimplifiedQuestion(Question question){
        var questionRequest = EntityDtoMapper.toQuestionRequest(question);
        return geminiService.generateResponse(questionSimplifiedQuestion
                .replace("{question}", questionRequest.toString()))
            .map(JsonStringUtil::extractJsonFromResponse)
            .map(jsonResponse -> parseSimplifiedQuestion(jsonResponse, questionRequest))
            .flatMap(questionService::saveSimplifiedQuestion);
    }

    private QuestionRequest parseResponse(String jsonResponse, QuestionRequest originalRequest) {
        try {
            // Create a temporary class for parsing the specific response format
            var parsedResponse = parseJsonWithFallback(jsonResponse, QuestionResponse.class, null);
            
            if (parsedResponse != null) {
                String question = parsedResponse.question() != null ? parsedResponse.question() : originalRequest.question();
                
                return QuestionRequest.builder()
                    .category(originalRequest.category())
                    .expYears(originalRequest.expYears())
                    ._id(originalRequest._id())
                    .userAnswer(originalRequest.userAnswer())
                    .modelAnswer(parsedResponse.modelAnswer())
                    .score(parsedResponse.score())
                    .question(question)
                    .additionalQuestions(originalRequest.additionalQuestions())
                    .build();
            } else {
                log.warn("Failed to parse response, returning original request");
                return originalRequest;
            }
            
        } catch (Exception e) {
            log.error("Unexpected error in parseResponse: {}", e.getMessage());
            return originalRequest;
        }
    }
    
    // Helper record for parsing specific response formats
    private record QuestionResponse(
        String question,
        String modelAnswer,
        Integer score
    ) {}

    private QuestionRequest parseAdditionalQuestionsAndAnswers(String jsonResponse, QuestionRequest originalRequest){
        try {
            String cleanJson = JsonStringUtil.extractJsonFromResponse(jsonResponse);
            log.debug("Parsing additional questions from JSON: {}", cleanJson);
            
            // Try multiple parsing strategies
            List<AdditionalQuestion> additionalQuestionsList = parseAdditionalQuestionsWithFallback(cleanJson);
            
            log.info("Successfully parsed {} additional questions", additionalQuestionsList.size());
            
            // Build new QuestionRequest with original data + additional questions
            return QuestionRequest.builder()
                ._id(originalRequest._id())
                .question(originalRequest.question())
                .userAnswer(originalRequest.userAnswer())
                .modelAnswer(originalRequest.modelAnswer())
                .category(originalRequest.category())
                .expYears(originalRequest.expYears())
                .score(originalRequest.score())
                .additionalQuestions(additionalQuestionsList)
                .build();
                
        } catch (Exception e) {
            log.error("Failed to parse additional questions JSON response: {}", jsonResponse, e);
            // Return original request with empty additional questions list instead of null
            return QuestionRequest.builder()
                ._id(originalRequest._id())
                .question(originalRequest.question())
                .userAnswer(originalRequest.userAnswer())
                .modelAnswer(originalRequest.modelAnswer())
                .category(originalRequest.category())
                .expYears(originalRequest.expYears())
                .score(originalRequest.score())
                .additionalQuestions(List.of()) // Empty list instead of null
                .build();
        }
    }
    
    private List<AdditionalQuestion> parseAdditionalQuestionsWithFallback(String cleanJson) {
        // Strategy 1: Standard JSON parsing
        try {
            AdditionalQuestion[] array = objectMapper.readValue(cleanJson, AdditionalQuestion[].class);
            return Arrays.asList(array);
        } catch (JsonProcessingException e1) {
            log.warn("Standard JSON parsing failed, trying enhanced cleaning: {}", e1.getMessage());
            
            // Strategy 2: Enhanced JSON cleaning
            try {
                String enhancedCleanJson = JsonStringUtil.extractAndCleanJsonFromResponse(cleanJson);
                AdditionalQuestion[] array = objectMapper.readValue(enhancedCleanJson, AdditionalQuestion[].class);
                return Arrays.asList(array);
            } catch (JsonProcessingException e2) {
                log.warn("Enhanced JSON cleaning failed, trying manual parsing: {}", e2.getMessage());
                
                // Strategy 3: Manual parsing as last resort
                return parseAdditionalQuestionsManually(cleanJson);
            }
        }
    }
    
    private List<AdditionalQuestion> parseAdditionalQuestionsManually(String json) {
        List<AdditionalQuestion> result = new ArrayList<>();
        try {
            // Extract question-answer pairs using regex
            Pattern pattern = Pattern.compile("\"question\"\\s*:\\s*\"([^\"]*(?:\\\\.[^\"]*)*)\",?\\s*\"answer\"\\s*:\\s*\"([^\"]*(?:\\\\.[^\"]*)*?)\"", Pattern.DOTALL);
            Matcher matcher = pattern.matcher(json);
            
            while (matcher.find()) {
                String question = matcher.group(1).replaceAll("\\\\\"", "\"");
                String answer = matcher.group(2).replaceAll("\\\\\"", "\"");
                
                result.add(AdditionalQuestion.builder()
                    .question(question)
                    .answer(answer)
                    .build());
            }
            
            log.info("Manual parsing extracted {} questions", result.size());
            
        } catch (Exception e) {
            log.error("Manual parsing also failed: {}", e.getMessage());
        }
        
        return result;
    }

    private SimplifiedQuestionRequest parseSimplifiedQuestion(String jsonResponse, QuestionRequest originalRequest){
        // Create fallback value
        SimplifiedQuestionRequest fallback = SimplifiedQuestionRequest.builder()
            ._id(originalRequest._id())
            .question(originalRequest.question())
            .category(originalRequest.category())
            .expYears(originalRequest.expYears())
            .simplifiedDetail("요약 생성 실패")
            .build();
            
        try {
            var result = parseJsonWithFallback(jsonResponse, SimplifiedQuestionRequest.class, fallback);
            
            // Validate essential fields
            if (result != null && result.question() != null) {
                return result;
            } else {
                log.warn("Parsed SimplifiedQuestionRequest has null essential fields, using fallback");
                return fallback;
            }
            
        } catch (Exception e) {
            log.error("Unexpected error in parseSimplifiedQuestion: {}", e.getMessage());
            return fallback;
        }
    }

    /**
     * Common robust JSON parsing utility with multiple fallback strategies
     */
    private <T> T parseJsonWithFallback(String jsonResponse, Class<T> targetClass, T fallbackValue) {
        try {
            // Strategy 1: Standard JSON parsing
            String cleanJson = JsonStringUtil.extractJsonFromResponse(jsonResponse);
            log.debug("Parsing JSON for {}: {}", targetClass.getSimpleName(), cleanJson);
            return objectMapper.readValue(cleanJson, targetClass);
            
        } catch (JsonProcessingException e1) {
            log.warn("Standard JSON parsing failed for {}, trying enhanced cleaning: {}", targetClass.getSimpleName(), e1.getMessage());
            
            // Strategy 2: Enhanced JSON cleaning
            try {
                String enhancedCleanJson = JsonStringUtil.extractAndCleanJsonFromResponse(jsonResponse);
                return objectMapper.readValue(enhancedCleanJson, targetClass);
                
            } catch (JsonProcessingException e2) {
                log.error("Enhanced JSON cleaning also failed for {}: {}", targetClass.getSimpleName(), e2.getMessage());
                log.error("Failed JSON content: {}", jsonResponse);
                return fallbackValue;
            }
        } catch (Exception e) {
            log.error("Unexpected error while parsing JSON for {}: {}", targetClass.getSimpleName(), e.getMessage());
            log.error("Failed JSON content: {}", jsonResponse);
            return fallbackValue;
        }
    }

    private String questionCreationPrompt = """ 
            You are a developer interviewer.
            Please create a question that you would like to ask the developer of year {expYears} about technology stack {category} for the interview.
            In addition, please refer to the past records to give new question or advanced question.
            answers should be korean.
            ===========history===========
            {history}
            ===========history===========
             Please respond with ONLY a valid JSON object in this exact format:
            {
                "question": "question"
            }            
    """;

    private String questionGradingPrompt ="""
            You are a developer interviewer.
            Please grade the question with the user's answer.
            consider developer's experience years making model answer to grade the question.
            Score will be 0 to 100.
            answers should be korean.
            The model answer should be written with a clear example of what needs to be pointed out and what needs to be supplemented.
            
            IMPORTANT JSON FORMATTING RULES:
            1. All strings must be properly escaped (use \\" for quotes inside strings)
            2. No line breaks inside string values (use \\n instead)
            3. Keep model answer concise but informative (max 800 characters)
            4. Avoid special characters like $ in string values
            5. Ensure valid JSON format
            
            ===========question===========
            {question}
            ===========question===========
            Please respond with ONLY a valid JSON object in this exact format:
            {
                "score": 85,
                "modelAnswer": "모델 답변 내용 (특수문자는 적절히 이스케이프 처리)"
            }
            """;

    private String questionAdditionalQuestion = """
            You are a developer interviewer.
            The records are about the questions and answers that have been asked, and the internal additional question is about the additional questions and answers that have been asked.
            now you making a new AdditionalQuestion's answer.
            Fill in the unanswered content of the additional request and return all the content of the additional request in the form of Array of json
            The answer should be explained with clear evidence and examples regarding the parts you point out.
            and answer should be korean.
            
            IMPORTANT JSON FORMATTING RULES:
            1. All strings must be properly escaped (use \\" for quotes inside strings)
            2. No line breaks inside string values (use \\n instead)
            3. Keep answers concise but informative (max 500 characters per answer)
            4. Ensure valid JSON array format
            
            ===========records===========
            {records}
            ===========records===========
            Please respond with ONLY a valid JSON array in this exact format:
            [
                {
                    "question": "질문 내용",
                    "answer": "답변 내용 (특수문자는 적절히 이스케이프 처리)"
                }
            ]

            """;

    private String questionSimplifiedQuestion = """
            It's about developer interview.
            simplify the question and answer in simplifiedDetail
            limit 100 words.
            ===========question===========
            {question}
            ===========question===========
            Please respond with ONLY a valid JSON object in this exact format:
            {
                "_id": "_id",
                "question": "question",
                "simplifiedDetail": "simplifiedDetail",
                "category": "category",
                "expYears": "expYears"
            }
            """;

            
}
