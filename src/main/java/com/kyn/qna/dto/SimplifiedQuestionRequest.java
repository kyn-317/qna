package com.kyn.qna.dto;

import lombok.Builder;

@Builder
public record SimplifiedQuestionRequest(
    String _id,
    String question,
    String simplifiedDetail,
    String category,
    Integer expYears
) {
    
}
