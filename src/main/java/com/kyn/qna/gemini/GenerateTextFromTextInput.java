package com.kyn.qna.gemini;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;

public class GenerateTextFromTextInput {
    public static void main(String[] args) {
        // The client gets the API key from the environment variable `GOOGLE_API_KEY`.
        Client client = new Client();
    
        GenerateContentResponse response =
            client.models.generateContent(
                "gemini-2.0-flash",
                "Explain how AI works in a few words",
                null);
    
        System.out.println(response.text());
      }
}
