package com.kyn.qna.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Data
@Document(collection = "questions")
public class Question {

    @Id
    private String id;
    private String text;
    private String technologyStack;
    private String experienceLevel;
    private String generatedBy;
    private LocalDateTime createdAt;
}
