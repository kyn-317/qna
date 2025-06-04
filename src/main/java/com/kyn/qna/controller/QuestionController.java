package com.kyn.qna.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kyn.qna.dto.QuestionRequest;
import com.kyn.qna.entity.Question;
import com.kyn.qna.service.QuestionManageService;

import reactor.core.publisher.Mono;

@RestController
@RequestMapping("question")
public class QuestionController {
    private final QuestionManageService questionManageService;

    public QuestionController(QuestionManageService questionManageService) {
        this.questionManageService = questionManageService;
    }
    
    
    @PostMapping
    public Mono<Question> createQuestion(@RequestBody QuestionRequest questionRequest) {
        return questionManageService.createQuestion(questionRequest);
    }

    @PostMapping("answer")
    public Mono<Question> updateQuestion(@RequestBody QuestionRequest questionRequest) {
        return questionManageService.userAnswered(questionRequest);
    }

    @PostMapping("additional-question")
    public Mono<Question> getAdditionalQuestionsAndAnswers(@RequestBody QuestionRequest questionRequest) {
        return questionManageService.getAdditionalQuestionsAndAnswers(questionRequest);
    }

}
