package com.kyn.qna.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.kyn.qna.dto.RequestDto;
import com.kyn.qna.service.GeminiService;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
public class GeminiController {
    
    private final GeminiService geminiService;

    public GeminiController(GeminiService geminiService) {
        this.geminiService = geminiService;
    }

    @PostMapping("generate")
    public Mono<String> generate(@RequestBody RequestDto requestDto) throws Exception{
        log.info("Generating response for question: {}", requestDto);
        return geminiService.generateResponse(requestDto.getRequestString());
    }

    @PostMapping(value = "generate-stream", produces = "text/event-stream")
    public Flux<String> generateStream(@RequestBody RequestDto requestDto) {
        log.info("Generating response for question: {}", requestDto.getRequestString());
        return geminiService.generateResponseStream(requestDto.getRequestString());
    }
}
