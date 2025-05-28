package com.kyn.qna.gemini;

import com.kyn.qna.model.Answer;
import com.kyn.qna.model.Evaluation;
import com.kyn.qna.model.Question;
import com.google.genai.Client;
import com.google.genai.Models;
import com.google.genai.ResponseStream;
import com.google.genai.types.Candidate;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnswerEvaluationServiceTest {

    @Mock
    private Client googleGenaiClient;

    @Mock
    private Models modelsApi;

    @Mock
    private ResponseStream<GenerateContentResponse> responseStream;

    @Mock
    private GenerateContentResponse generateContentResponse;

    @Mock
    private Candidate candidate;

    @Mock
    private Content responseContent;

    @Mock
    private Part responsePart;

    @InjectMocks
    private AnswerEvaluationService answerEvaluationService;

    private Question sampleQuestion;
    private Answer sampleAnswer;

    @BeforeEach
    void setUp() {
        sampleQuestion = new Question();
        sampleQuestion.setId("q1");
        sampleQuestion.setText("What is polymorphism?");

        sampleAnswer = new Answer();
        sampleAnswer.setId("a1");
        sampleAnswer.setQuestionId("q1");
        sampleAnswer.setText("Polymorphism allows objects to take on many forms.");

        // Common mocking for googleGenaiClient.getModels()
        lenient().when(googleGenaiClient.getModels()).thenReturn(modelsApi);
    }

    private void setupGeminiResponse(String rawResponseText) {
        when(modelsApi.generateContentStream(anyString(), any(List.class), any())).thenReturn(responseStream);
        when(responseStream.iterator()).thenReturn(Collections.singletonList(generateContentResponse).iterator());
        when(generateContentResponse.candidates()).thenReturn(Optional.of(Collections.singletonList(candidate)));
        when(candidate.content()).thenReturn(Optional.of(responseContent));
        when(responseContent.parts()).thenReturn(Optional.of(Collections.singletonList(responsePart)));
        when(responsePart.text()).thenReturn(rawResponseText);
    }

    @Test
    void testEvaluateAnswer_Success_Correct() {
        String geminiResponse = "Evaluation: Correct\n" +
                                "Feedback: The answer is comprehensive and accurate.\n" +
                                "Exemplary Answer: This is a model answer that covers all aspects.";
        setupGeminiResponse(geminiResponse);

        Evaluation result = answerEvaluationService.evaluateAnswer(sampleQuestion, sampleAnswer);

        assertNotNull(result);
        assertEquals(1.0, result.getScore());
        assertEquals("The answer is comprehensive and accurate.", result.getFeedback());
        assertEquals("This is a model answer that covers all aspects.", result.getExemplaryAnswer());
        assertEquals("q1", result.getQuestionId());
        assertEquals("a1", result.getAnswerId());
        assertEquals("gemini-1.5-flash-latest", result.getEvaluatedBy());
        assertNotNull(result.getCreatedAt());

        ArgumentCaptor<List<Content>> contentCaptor = ArgumentCaptor.forClass(List.class);
        verify(modelsApi).generateContentStream(eq("gemini-1.5-flash-latest"), contentCaptor.capture(), any());
        String promptSent = contentCaptor.getValue().get(0).getParts().get(0).getText();
        assertTrue(promptSent.contains(sampleQuestion.getText()));
        assertTrue(promptSent.contains(sampleAnswer.getText()));
    }

    @Test
    void testEvaluateAnswer_Success_PartiallyCorrect() {
        String geminiResponse = "Evaluation: Partially Correct\n" +
                                "Feedback: Some aspects are missing.\n" +
                                "Exemplary Answer: A more complete answer would include...";
        setupGeminiResponse(geminiResponse);

        Evaluation result = answerEvaluationService.evaluateAnswer(sampleQuestion, sampleAnswer);

        assertEquals(0.5, result.getScore());
        assertEquals("Some aspects are missing.", result.getFeedback());
        assertEquals("A more complete answer would include...", result.getExemplaryAnswer());
    }
    
    @Test
    void testEvaluateAnswer_Success_PartiallyCorrect_MultiLine() {
        String geminiResponse = "Evaluation: Partially Correct\n" +
                                "Feedback: Some aspects are missing.\nLine 2 of feedback.\n" +
                                "Exemplary Answer: A more complete answer would include...\nLine 2 of exemplary answer.";
        setupGeminiResponse(geminiResponse);

        Evaluation result = answerEvaluationService.evaluateAnswer(sampleQuestion, sampleAnswer);

        assertEquals(0.5, result.getScore());
        assertEquals("Some aspects are missing.\nLine 2 of feedback.", result.getFeedback());
        assertEquals("A more complete answer would include...\nLine 2 of exemplary answer.", result.getExemplaryAnswer());
    }


    @Test
    void testEvaluateAnswer_Success_Incorrect() {
        String geminiResponse = "Evaluation: Incorrect\n" +
                                "Feedback: The answer is fundamentally flawed.\n" +
                                "Exemplary Answer: The correct approach is...";
        setupGeminiResponse(geminiResponse);

        Evaluation result = answerEvaluationService.evaluateAnswer(sampleQuestion, sampleAnswer);

        assertEquals(0.0, result.getScore());
        assertEquals("The answer is fundamentally flawed.", result.getFeedback());
        assertEquals("The correct approach is...", result.getExemplaryAnswer());
    }

    @Test
    void testEvaluateAnswer_ParsingError_MissingFields() {
        String geminiResponse = "Evaluation: Correct\n" +
                                // Missing Feedback line
                                "Exemplary Answer: This is a model answer.";
        setupGeminiResponse(geminiResponse);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> answerEvaluationService.evaluateAnswer(sampleQuestion, sampleAnswer));
        assertTrue(exception.getMessage().contains("Failed to parse structured response from Gemini"));
    }

    @Test
    void testEvaluateAnswer_ParsingError_EvaluationTextUnmapped() {
        String geminiResponse = "Evaluation: Highly Recommended\n" +
                                "Feedback: Great answer!\n" +
                                "Exemplary Answer: Perfect.";
        setupGeminiResponse(geminiResponse);

        Evaluation result = answerEvaluationService.evaluateAnswer(sampleQuestion, sampleAnswer);

        assertEquals(-1.0, result.getScore()); // Default for unmapped
        assertEquals("Great answer!", result.getFeedback());
        assertEquals("Perfect.", result.getExemplaryAnswer());
    }

    @Test
    void testEvaluateAnswer_GeminiApiError() {
        when(modelsApi.generateContentStream(anyString(), any(List.class), any())).thenThrow(new RuntimeException("Gemini network error"));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> answerEvaluationService.evaluateAnswer(sampleQuestion, sampleAnswer));
        assertTrue(exception.getMessage().contains("Error evaluating answer via Gemini API"));
        assertTrue(exception.getCause().getMessage().contains("Gemini network error"));
    }

    @Test
    void testEvaluateAnswer_GeminiReturnsEmptyResponse() {
        setupGeminiResponse(""); // Empty string from Gemini

        RuntimeException exception = assertThrows(RuntimeException.class, () -> answerEvaluationService.evaluateAnswer(sampleQuestion, sampleAnswer));
        assertEquals("Gemini API returned an empty response.", exception.getMessage());
    }
}
