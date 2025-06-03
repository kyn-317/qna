package com.kyn.qna.service;

import com.kyn.qna.dto.QuestionRequest;
import com.kyn.qna.util.JsonStringUtil;

import reactor.core.publisher.Mono;

public class QuestionManageService {
    private final QuestionService questionService;
    private final GeminiService geminiService;

    public QuestionManageService(QuestionService questionService, GeminiService geminiService) {
        this.questionService = questionService;
        this.geminiService = geminiService;
    }

    public Mono<String> createQuestion(QuestionRequest questionRequest){

        return questionService.getQuestionsByCategory(questionRequest.category())
            .collectList()
            .map(questions -> 
                questionCreationPrompt
                .replace("{history}", questions.toString())
                .replace("{category}", questionRequest.category())
                .replace("{expYears}", String.valueOf(questionRequest.expYears())))    
            .flatMap(geminiService::generateResponse)
            .map(JsonStringUtil::extractJsonFromResponse);
    }



    private String questionCreationPrompt = """ 
            You are a developer interviewer.
            Please create a question that you would like to ask the developer of year {expYears} about technology {category} for the interview.
            In addition, please refer to the past records to give new question or advanced question.
            ===========history===========
            {history}
            ===========history===========
             Please respond with ONLY a valid JSON object in this exact format:
            {
                "question": "question"
            }            
    """;

}
