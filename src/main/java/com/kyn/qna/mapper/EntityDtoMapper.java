package com.kyn.qna.mapper;



import java.time.Instant;
import java.time.LocalDateTime;

import com.kyn.qna.dto.CategoryRequest;
import com.kyn.qna.entity.Category; 

public class EntityDtoMapper {
    
    public static Category toCategory(CategoryRequest categoryRequest) {
        return Category.builder()
            .name(categoryRequest.getName())
            .description(categoryRequest.getDescription())
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
    }
    
}
