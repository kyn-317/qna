package com.kyn.qna.gemini;

import com.kyn.qna.model.Answer;
import com.kyn.qna.model.Evaluation;
import com.kyn.qna.model.Question;
import com.google.common.collect.ImmutableList;
import com.google.genai.Client; // Import Client
import com.google.genai.ResponseStream;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
// import org.springframework.beans.factory.annotation.Value; // No longer needed for API key here
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AnswerEvaluationService {

    private static final Logger logger = LoggerFactory.getLogger(AnswerEvaluationService.class);
    private final Client geminiClient; // Injected directly
    private final String modelName = "gemini-1.5-flash-latest";

    // Constructor updated to accept Client
    public AnswerEvaluationService(Client geminiClient) {
        this.geminiClient = geminiClient;
    }

    public Evaluation evaluateAnswer(Question question, Answer userAnswer) {
        logger.info("Starting answer evaluation for question ID: {} and answer ID: {}", question.getId(), userAnswer.getId());

        String prompt = buildEvaluationPrompt(question.getText(), userAnswer.getText());
        logger.debug("Generated evaluation prompt for Gemini: {}", prompt);

        try {
            GenerateContentConfig config = GenerateContentConfig.builder()
                    .responseMimeType("text/plain")
                    .build();

            List<Content> contents = ImmutableList.of(
                    Content.builder()
                            .role("user")
                            .parts(ImmutableList.of(Part.fromText(prompt)))
                            .build()
            );
            // Use the injected geminiClient
            ResponseStream<GenerateContentResponse> responseStream = geminiClient.getModels().generateContentStream(modelName, contents, config);
            StringBuilder generatedTextBuilder = new StringBuilder();
            for (GenerateContentResponse res : responseStream) {
                if (res.candidates().isEmpty() || res.candidates().get().get(0).content().isEmpty() || res.candidates().get().get(0).content().get().parts().isEmpty()) {
                    continue;
                }
                List<Part> parts = res.candidates().get().get(0).content().get().parts().get();
                for (Part part : parts) {
                    if (part.text() != null) {
                        generatedTextBuilder.append(part.text());
                    }
                }
            }
            responseStream.close();

            String rawResponse = generatedTextBuilder.toString().trim();
            logger.debug("Raw response from Gemini: {}", rawResponse);

            if (rawResponse.isEmpty()) {
                logger.error("Gemini API returned an empty response for question ID: {} and answer ID: {}", question.getId(), userAnswer.getId());
                throw new RuntimeException("Gemini API returned an empty response.");
            }

            Map<String, String> parsedResponse = parseStructuredResponse(rawResponse);

            String qualitativeEvaluation = parsedResponse.get("Evaluation");
            String feedback = parsedResponse.get("Feedback");
            String exemplaryAnswer = parsedResponse.get("Exemplary Answer");

            if (qualitativeEvaluation == null || feedback == null || exemplaryAnswer == null) {
                logger.error("Failed to parse structured response from Gemini. Raw response: {}", rawResponse);
                throw new RuntimeException("Failed to parse structured response from Gemini. One or more fields are missing.");
            }
            
            double score = convertQualitativeEvaluationToScore(qualitativeEvaluation);

            Evaluation evaluation = new Evaluation();
            evaluation.setQuestionId(question.getId());
            evaluation.setAnswerId(userAnswer.getId());
            evaluation.setScore(score);
            evaluation.setFeedback(feedback);
            evaluation.setExemplaryAnswer(exemplaryAnswer);
            evaluation.setEvaluatedBy(modelName);
            evaluation.setCreatedAt(LocalDateTime.now());

            logger.info("Successfully evaluated answer for question ID: {} and answer ID: {}. Score: {}", question.getId(), userAnswer.getId(), score);
            return evaluation;

        } catch (Exception e) {
            logger.error("Error during answer evaluation for question ID: {} and answer ID: {}", question.getId(), userAnswer.getId(), e);
            throw new RuntimeException("Error evaluating answer via Gemini API: " + e.getMessage(), e);
        }
    }

    private String buildEvaluationPrompt(String questionText, String userAnswerText) {
        return String.format(
                "You are an expert technical interviewer. Evaluate the following answer provided by a candidate for the given question.\n" +
                "Provide a qualitative evaluation (e.g., Correct, Partially Correct, Incorrect), brief feedback, and a model exemplary answer.\n\n" +
                "Question:\n" +
                "\"%s\"\n\n" +
                "Candidate's Answer:\n" +
                "\"%s\"\n\n" +
                "Respond STRICTLY in the following format, and do not deviate:\n" +
                "Evaluation: [Your qualitative evaluation of the candidate's answer]\n" +
                "Feedback: [Your feedback on the candidate's answer]\n" +
                "Exemplary Answer: [Your model answer to the question]",
                questionText, userAnswerText
        );
    }

    private Map<String, String> parseStructuredResponse(String response) {
        Map<String, String> parsedMap = new HashMap<>();
        String[] lines = response.split("\\r?\\n"); 

        String currentKey = null;
        StringBuilder currentValue = new StringBuilder();

        for (String line : lines) {
            if (line.startsWith("Evaluation:")) {
                if (currentKey != null) parsedMap.put(currentKey, currentValue.toString().trim());
                currentKey = "Evaluation";
                currentValue = new StringBuilder(line.substring(currentKey.length() + 1).trim());
            } else if (line.startsWith("Feedback:")) {
                if (currentKey != null) parsedMap.put(currentKey, currentValue.toString().trim());
                currentKey = "Feedback";
                currentValue = new StringBuilder(line.substring(currentKey.length() + 1).trim());
            } else if (line.startsWith("Exemplary Answer:")) {
                if (currentKey != null) parsedMap.put(currentKey, currentValue.toString().trim());
                currentKey = "Exemplary Answer";
                currentValue = new StringBuilder(line.substring(currentKey.length() + 1).trim());
            } else if (currentKey != null) {
                currentValue.append("\n").append(line); // Append subsequent lines to the current field
            }
        }
        if (currentKey != null) { // Put the last parsed field
            parsedMap.put(currentKey, currentValue.toString().trim());
        }
        
        if (!parsedMap.containsKey("Evaluation") || !parsedMap.containsKey("Feedback") || !parsedMap.containsKey("Exemplary Answer")) {
            logger.warn("Potential parsing issue: Not all expected fields (Evaluation, Feedback, Exemplary Answer) found. Raw response: {}", response);
        }
        return parsedMap;
    }

    private double convertQualitativeEvaluationToScore(String qualitativeEvaluation) {
        if (qualitativeEvaluation == null) {
            return -1.0; 
        }
        switch (qualitativeEvaluation.toLowerCase().trim()) {
            case "correct":
                return 1.0;
            case "partially correct":
                return 0.5;
            case "incorrect":
                return 0.0;
            default:
                logger.warn("Unknown qualitative evaluation: '{}'. Defaulting to score -1.0.", qualitativeEvaluation);
                return -1.0; 
        }
    }
}
