package com.kyn.qna.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryResult {
    
    @JsonProperty("shouldUpdate")
    private boolean shouldUpdate;
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("description")
    private String description;
    
} 