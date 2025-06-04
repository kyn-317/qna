package com.kyn.qna.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "simplifiedQuestion")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SimplifiedQuestion {
    @Id
    private String _id;
    private String question;
    private String simplifiedDetail;
    private String category;
    private Integer expYears;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
