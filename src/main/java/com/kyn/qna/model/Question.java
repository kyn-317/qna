package com.kyn.qna.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Document(collection = "questions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Question {
    @Id
    private String _id;
    
    private String title;
    private String content;
    private String category;
    private String author;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean isAnswered;
    private int viewCount;
} 