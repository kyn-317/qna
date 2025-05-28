package com.kyn.qna.controller.dto;

import lombok.Data;

@Data
public class SubmitAnswerRequest {
    private String sessionId;
    private String questionId;
    // private String questionText; // Removed as per subtask instructions
    private String answerText;
}
