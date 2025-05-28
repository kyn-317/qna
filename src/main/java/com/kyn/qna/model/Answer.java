package com.kyn.qna.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Data
@Document(collection = "answers")
public class Answer {

    @Id
    private String id;
    private String questionId; // Keeping it simple as String for now
    private String text;
    private String answeredBy;
    private LocalDateTime createdAt;
}
