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

    public Mono<String> generateResponse(String question) {   
        
        log.info("Generating response for question: {}", question);
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

}
