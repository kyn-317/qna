package com.kyn.qna.service;

import java.lang.invoke.TypeDescriptor;
import java.lang.reflect.Method;
import java.security.PrivateKey;
import java.util.List;
import java.util.Optional;

import org.springframework.data.util.ParameterTypes;
import org.springframework.stereotype.Service;

import com.google.common.collect.ImmutableList;
import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import com.google.genai.types.Tool;
import com.google.genai.types.ToolCodeExecution;
import com.kyn.qna.entity.Question;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class GeminiService {
    
    private final CategoryService categoryService;
    private final Client aiClient;
   
    public GeminiService(Client aiClient, CategoryService categoryService) {
        this.aiClient = aiClient;
        this.categoryService = categoryService;
    }

    public Mono<String> generateResponse(String question) throws Exception {   
        
        log.info("Generating response for question: {}", question);
        List<Content> contents = ImmutableList.of(
            Content.builder()
              .role("user")
              .parts(ImmutableList.of(
                Part.fromText(question)
              ))
              .build()
          );

          
          
          Method findByCategoryMethod =
           QuestionService.class.getMethod(
            "findByCategory",String.class
          );
          GenerateContentConfig config =
          GenerateContentConfig
          .builder()
          .responseMimeType("text/plain")
          .tools(List.of(Tool.builder()
          .functions(ImmutableList.of(findByCategoryMethod))
          .build()))
          .build();

          log.info("Generating response for question: {}", question);

        return Mono.fromFuture(aiClient.async.models.generateContent("gemma-3n-e4b-it", contents, config))
            .filter(response -> response.candidates().isPresent() && !response.candidates().get().isEmpty())
            .map(response -> response.candidates().get().get(0))
            .filter(candidate -> candidate.content().isPresent())
            .map(candidate -> candidate.content().get())
            .filter(content -> content.parts().isPresent() && !content.parts().get().isEmpty())
            .map(content -> content.parts().get().get(0).text())
            .filter(Optional::isPresent)
            .map(Optional::get);
            
    }

    public Flux<String> generateResponseStream(String question) {
        List<Content> contents = ImmutableList.of(
            Content.builder()
              .role("user")
              .parts(ImmutableList.of(
                Part.fromText(question)
              ))
              .build()
          );

          GenerateContentConfig config =
          GenerateContentConfig
          .builder()
          .responseMimeType("text/plain")
          .build();

        return Mono.fromFuture(aiClient.async.models.generateContentStream("gemma-3n-e4b-it", contents, config))
            .flatMapMany(stream -> Flux.fromIterable(stream))
            .map(GenerateContentResponse::candidates)
            .filter(candidates -> candidates.isPresent() && !candidates.get().isEmpty())
            .map(candidates -> candidates.get().get(0))
            .filter(candidate -> candidate.content().isPresent())
            .map(candidate -> candidate.content().get())
            .filter(content -> content.parts().isPresent() && !content.parts().get().isEmpty())
            .map(content -> content.parts().get().get(0).text())
            .filter(Optional::isPresent)
            .map(Optional::get);
    }

    public Mono<String> manageCategory(String question){
        log.info("Processing category management request: {}", question);
        
        List<Content> contents = ImmutableList.of(
            Content.builder()
              .role("user")
              .parts(ImmutableList.of(
                Part.fromText(createEnhancedPrompt(question))
              ))
              .build()
          );

        GenerateContentConfig config = GenerateContentConfig
            .builder()
            .responseMimeType("text/plain")
            .build();
        
        return Mono.fromFuture(aiClient.async.models.generateContent("gemini-1.5-flash", contents, config))
            .filter(response -> response.candidates().isPresent() && !response.candidates().get().isEmpty())
            .map(response -> response.candidates().get().get(0))
            .filter(candidate -> candidate.content().isPresent())
            .map(candidate -> candidate.content().get())
            .filter(content -> content.parts().isPresent() && !content.parts().get().isEmpty())
            .map(content -> content.parts().get().get(0).text())
            .filter(Optional::isPresent)
            .map(Optional::get)
            .doOnNext(response -> log.info("Received response from Gemini: {}", response))
            .onErrorResume(error -> {
                log.error("Error calling Gemini API", error);
                return Mono.just("Error occurred while processing the request: " + error.getMessage());
            });
    }

    /**
     * 카테고리 관리를 위한 향상된 프롬프트 생성
     */
    private String createEnhancedPrompt(String originalPrompt) {
        return String.format("""
            당신은 카테고리 관리 전문가입니다. 다음 요청을 처리해주세요:
            
            %s
            
            다음 작업을 수행할 수 있습니다:
            1. 카테고리 목록 조회 및 분석
            2. 부적절한 카테고리 description 식별
            3. 개선된 description 제안
            4. 카테고리 업데이트 실행
            
            응답 형식:
            - 분석 결과를 상세히 설명
            - 개선이 필요한 항목과 이유 명시
            - 구체적인 개선 방안 제시
            - 실행된 변경사항 요약
            
            한국어로 답변해주세요.
            """, originalPrompt);
    }
}
