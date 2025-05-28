package com.kyn.qna.controller.dto;

import lombok.Data;

@Data
public class StartSessionRequest {
    private String technologyStack;
    private String experienceLevel;
    private String userId;
}
