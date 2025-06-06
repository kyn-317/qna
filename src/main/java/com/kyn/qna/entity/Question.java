package com.kyn.qna.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import com.kyn.qna.dto.AdditionalQuestion;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "question")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Question {
    @Id
    private String _id;
    
    private String question;
    private String userAnswer;
    private String modelAnswer;
    private String category;
    private Integer expYears;
    private Integer score;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<AdditionalQuestion> additionalQuestions;
} 