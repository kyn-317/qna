package com.kyn.qna.util;

public class JsonStringUtil {
    
    public static String extractJsonFromResponse(String response) {

        String cleaned = response.trim();
        
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7); 
        }
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3); 
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3); 
        }
        
        return cleaned.trim();
    }
}
