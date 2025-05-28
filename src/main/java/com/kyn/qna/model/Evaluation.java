package com.kyn.qna.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Data
@Document(collection = "evaluations")
public class Evaluation {

    @Id
    private String id;
    private String questionId;
    private String answerId;
    private double score;
    private String feedback;
    private String exemplaryAnswer;
    private String evaluatedBy;
    private LocalDateTime createdAt;
}
