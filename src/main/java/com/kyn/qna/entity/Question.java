
package com.kyn.qna.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

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
    private int expYears;
    private int score;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
} 