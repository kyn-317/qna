package com.kyn.qna.dto;

import com.google.auto.value.AutoValue.Builder;

@Builder
public record QuestionRequest(String category, int expYears) {
    
}
