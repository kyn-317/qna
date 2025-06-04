package com.kyn.qna.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdditionalQuestion {
    @JsonProperty("question")
    private String question;
    
    @JsonProperty("answer")
    private String answer;
}
