package com.kyn.qna.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryResult {
    
    @JsonProperty("shouldUpdate")
    private boolean shouldUpdate;
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("description")
    private String description;
    
} 