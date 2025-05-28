package com.kyn.qna.controller.dto;

import lombok.Data;

@Data
public class AcceptAnswerRequest {
    private String sessionId;
    private String questionId;
    private String answerId;
    private String evaluationId;
}
