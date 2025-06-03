package com.kyn.qna.dto;

import lombok.Builder;

@Builder
public record CategoryRequest(String name, String description) {
}
