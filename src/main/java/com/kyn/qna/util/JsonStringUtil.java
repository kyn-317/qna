package com.kyn.qna.util;

import org.springframework.http.codec.ServerCodecConfigurer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

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

    /**
     * Enhanced JSON extraction for AI-generated responses
     * Handles malformed JSON with better error recovery
     */
    public static String extractAndCleanJsonFromResponse(String response) {
        String cleaned = extractJsonFromResponse(response);
        
        // Try to fix common JSON issues
        cleaned = fixCommonJsonIssues(cleaned);
        
        return cleaned;
    }
    
    private static String fixCommonJsonIssues(String json) {
        // Remove any trailing commas before closing brackets/braces
        json = json.replaceAll(",\\s*([\\]}])", "$1");
        
        // Fix unescaped quotes in string values (basic approach)
        // This is a simplified fix - for production, you might need more sophisticated regex
        json = fixUnescapedQuotes(json);
        
        return json;
    }
    
    private static String fixUnescapedQuotes(String json) {
        // This is a basic approach to fix unescaped quotes
        // Look for patterns like "text with "quotes" inside"
        Pattern pattern = Pattern.compile("\"([^\"]*?)\"([^\"]*?)\"([^\"]*?)\"");
        Matcher matcher = pattern.matcher(json);
        
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String replacement = "\"" + matcher.group(1) + "\\\"" + matcher.group(2) + "\\\"" + matcher.group(3) + "\"";
            matcher.appendReplacement(sb, replacement);
        }
        matcher.appendTail(sb);
        
        return sb.toString();
    }


    public static ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    public static <T> T parseJson(String response, Class<T> clazz) {
        try {
            return objectMapper().readValue(extractJsonFromResponse(response), clazz);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse JSON: " + response, e);
        }
    }
}
