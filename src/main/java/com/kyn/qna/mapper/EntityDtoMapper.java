package com.kyn.qna.mapper;

import java.time.LocalDateTime;

import com.kyn.qna.dto.CategoryRequest;
import com.kyn.qna.dto.QuestionRequest;
import com.kyn.qna.dto.SimplifiedQuestionRequest;
import com.kyn.qna.entity.Category;
import com.kyn.qna.entity.Question;
import com.kyn.qna.entity.SimplifiedQuestion; 

public class EntityDtoMapper {
    
    public static Category toCategory(CategoryRequest categoryRequest) {
        return Category.builder()
            .name(categoryRequest.name())
            .description(categoryRequest.description())
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
    }
    
    public static Question toQuestion(QuestionRequest questionRequest) {
        return Question.builder()
            .category(questionRequest.category())
            .expYears(questionRequest.expYears())
            .question(questionRequest.question())
            .userAnswer(questionRequest.userAnswer())
            .modelAnswer(questionRequest.modelAnswer())
            .score(questionRequest.score())
            .build();
    }

    public static SimplifiedQuestion toSimplifiedQuestion(SimplifiedQuestionRequest request) {
        return SimplifiedQuestion.builder()
            ._id(request._id())
            .question(request.question())
            .simplifiedDetail(request.simplifiedDetail())
            .category(request.category())
            .expYears(request.expYears())
            .build();
    }

    public static QuestionRequest toQuestionRequest(Question question) {
        return QuestionRequest.builder()
            ._id(question.get_id())
            .question(question.getQuestion())
            .userAnswer(question.getUserAnswer())
            .modelAnswer(question.getModelAnswer())
            .score(question.getScore())
            .category(question.getCategory())
            .expYears(question.getExpYears())
            .build();
    }
}
