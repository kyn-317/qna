package com.kyn.qna.dto;

import java.util.List;

import lombok.Builder;

@Builder
public record QuestionRequest(
    String _id,
    String question,
    String userAnswer, 
    String modelAnswer,
    String category, 
    Integer expYears, 
    Integer score,
    List<AdditionalQuestion> additionalQuestions
    ) {
    
}
