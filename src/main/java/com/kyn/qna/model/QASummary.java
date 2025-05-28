package com.kyn.qna.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Data
@Document(collection = "qa_summaries")
public class QASummary {

    @Id
    private String id;

    @Indexed(unique = true)
    private String questionTextHash;

    private String technologyStack;
    private String experienceLevel;
    private String questionId;
    private LocalDateTime createdAt;
}
