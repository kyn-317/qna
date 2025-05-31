package com.kyn.qna.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.google.common.collect.ImmutableList;
import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class GeminiService {
    

    private final Client aiClient;
   
    public GeminiService(Client aiClient) {
        this.aiClient = aiClient;
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
