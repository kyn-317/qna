package com.kyn.qna.gemini;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.common.collect.ImmutableList;
import com.google.genai.Client;
import com.google.genai.ResponseStream;
import com.google.genai.types.*;
import com.google.gson.Gson;

@Service
public class GenerateTextFromTextInput {

    @Value("${GOOGLE_API_KEY}")
    private String googleApiKey;
    
    public void generateTextFromTextInput(String inputText) {
        // The client gets the API key from the environment variable `GOOGLE_API_KEY`.
        Client client = Client.builder().apiKey(googleApiKey).build();
        Gson gson = new Gson();
    
    
        String model = "gemma-3n-e4b-it";
        List<Content> contents = ImmutableList.of(
          Content.builder()
            .role("user")
            .parts(ImmutableList.of(
              Part.fromText(inputText)
            ))
            .build()
        );
        GenerateContentConfig config =
          GenerateContentConfig
          .builder()
          .responseMimeType("text/plain")
          .build();
    
        ResponseStream<GenerateContentResponse> responseStream = client.models.generateContentStream(model, contents, config);
    
        for (GenerateContentResponse res : responseStream) {
          if (res.candidates().isEmpty() || res.candidates().get().get(0).content().isEmpty() || res.candidates().get().get(0).content().get().parts().isEmpty()) {
            continue;
          }
    
          List<Part> parts = res.candidates().get().get(0).content().get().parts().get();
          for (Part part : parts) {
            System.out.println(part.text());
          }
        }
    
        responseStream.close();
      }
}


