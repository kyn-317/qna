package com.kyn.qna.service;

import com.kyn.qna.dto.QuestionRequest;
import com.kyn.qna.entity.Question;

import reactor.core.publisher.Mono;

public class QuestionManageService {
    private final QuestionService questionService;

    public QuestionManageService(QuestionService questionService) {
        this.questionService = questionService;
    }

    public Mono<Question> createQuestion(QuestionRequest questionRequest){
        return questionService.save(questionRequest);
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
