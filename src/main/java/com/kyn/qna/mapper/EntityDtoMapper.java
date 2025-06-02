package com.kyn.qna.mapper;

import java.time.LocalDateTime;

import com.kyn.qna.dto.CategoryRequest;
import com.kyn.qna.dto.QuestionRequest;
import com.kyn.qna.entity.Category;
import com.kyn.qna.entity.Question; 

public class EntityDtoMapper {
    
    public static Category toCategory(CategoryRequest categoryRequest) {
        return Category.builder()
            .name(categoryRequest.getName())
            .description(categoryRequest.getDescription())
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
    }
    
    public static Question toQuestion(QuestionRequest questionRequest) {
        return Question.builder()
            .category(questionRequest.category())
            .expYears(questionRequest.expYears())
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
    }
}
