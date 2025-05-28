package com.kyn.qna.controller;

import com.kyn.qna.controller.dto.AcceptAnswerRequest;
import com.kyn.qna.controller.dto.StartSessionRequest;
import com.kyn.qna.controller.dto.SubmitAnswerRequest;
import com.kyn.qna.gemini.AnswerEvaluationService;
import com.kyn.qna.gemini.QuestionGenerationService;
import com.kyn.qna.mcp.McpContextService;
import com.kyn.qna.model.Answer;
import com.kyn.qna.model.Evaluation;
import com.kyn.qna.model.QASummary;
import com.kyn.qna.model.Question;
import com.kyn.qna.repository.AnswerRepository;
import com.kyn.qna.repository.EvaluationRepository;
import com.kyn.qna.repository.QASummaryRepository;
import com.kyn.qna.repository.QuestionRepository;
import com.kyn.qna.service.HashService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/qna")
public class QnaController {

    private static final Logger logger = LoggerFactory.getLogger(QnaController.class);

    private final McpContextService mcpContextService;
    private final QuestionGenerationService questionGenerationService;
    private final AnswerEvaluationService answerEvaluationService;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final EvaluationRepository evaluationRepository;
    private final QASummaryRepository qaSummaryRepository;
    private final HashService hashService;

    public QnaController(McpContextService mcpContextService,
                         QuestionGenerationService questionGenerationService,
                         AnswerEvaluationService answerEvaluationService,
                         QuestionRepository questionRepository,
                         AnswerRepository answerRepository,
                         EvaluationRepository evaluationRepository,
                         QASummaryRepository qaSummaryRepository,
                         HashService hashService) {
        this.mcpContextService = mcpContextService;
        this.questionGenerationService = questionGenerationService;
        this.answerEvaluationService = answerEvaluationService;
        this.questionRepository = questionRepository;
        this.answerRepository = answerRepository;
        this.evaluationRepository = evaluationRepository;
        this.qaSummaryRepository = qaSummaryRepository;
        this.hashService = hashService;
    }

    @PostMapping("/session/start")
    public ResponseEntity<?> startSession(@RequestBody StartSessionRequest request) {
        try {
            String sessionId = UUID.randomUUID().toString();
            logger.info("Starting new session: {} for user: {}", sessionId, request.getUserId());
            mcpContextService.initializeContext(sessionId, request.getTechnologyStack(), request.getExperienceLevel(), request.getUserId());
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("sessionId", sessionId));
        } catch (Exception e) {
            logger.error("Error starting session for user {}: {}", request.getUserId(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Failed to start session: " + e.getMessage()));
        }
    }

    @GetMapping("/question")
    public ResponseEntity<?> getQuestion(@RequestParam String sessionId) {
        logger.info("Received request for a new question for session ID: {}", sessionId);
        try {
            Question generatedQuestion = questionGenerationService.generateQuestion(sessionId);
            // The question is already saved by QuestionGenerationService
            if (generatedQuestion != null && generatedQuestion.getId() != null) {
                mcpContextService.addQuestionToContext(sessionId, generatedQuestion);
                logger.info("Generated and saved question ID {} for session ID {}: {}", generatedQuestion.getId(), sessionId, generatedQuestion.getText());
                return ResponseEntity.ok(generatedQuestion);
            } else {
                logger.warn("Question generation or saving failed for session ID: {}", sessionId);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Failed to generate or save question."));
            }
        } catch (IllegalStateException e) {
            logger.error("Error generating question for session ID {}: {}", sessionId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Failed to generate question due to context issue: " + e.getMessage()));
        } catch (RuntimeException e) {
            logger.error("Runtime error generating question for session ID {}: {}", sessionId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "An unexpected error occurred while generating the question: " + e.getMessage()));
        }
    }

    @PostMapping("/answer")
    public ResponseEntity<?> submitAnswer(@RequestBody SubmitAnswerRequest request) {
        logger.info("Received answer submission for session ID: {} and question ID: {}", request.getSessionId(), request.getQuestionId());
        try {
            Optional<Question> questionOpt = questionRepository.findById(request.getQuestionId());
            if (questionOpt.isEmpty()) {
                logger.warn("Question not found for ID: {} in session ID: {}", request.getQuestionId(), request.getSessionId());
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Question not found."));
            }
            Question question = questionOpt.get();

            Answer answer = new Answer();
            answer.setQuestionId(request.getQuestionId());
            answer.setText(request.getAnswerText());
            answer.setAnsweredBy("client_user"); // Placeholder
            answer.setCreatedAt(LocalDateTime.now());

            Answer savedAnswer = answerRepository.save(answer);
            logger.info("Saved answer ID {} for question ID {}", savedAnswer.getId(), request.getQuestionId());

            mcpContextService.addAnswerToContext(request.getSessionId(), savedAnswer);

            Evaluation evaluation = answerEvaluationService.evaluateAnswer(question, savedAnswer);
            Evaluation savedEvaluation = evaluationRepository.save(evaluation); // Save evaluation
            logger.info("Answer evaluated and evaluation ID {} saved for session ID {}.", savedEvaluation.getId(), request.getSessionId());
            
            // Return the saved evaluation which now has its own ID
            return ResponseEntity.ok(savedEvaluation);

        } catch (IllegalStateException e) {
            logger.error("Context error submitting answer for session ID {}: {}", request.getSessionId(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Failed to submit answer due to context issue: " + e.getMessage()));
        } catch (RuntimeException e) {
            logger.error("Runtime error submitting answer for session ID {}: {}", request.getSessionId(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "An unexpected error occurred while submitting the answer: " + e.getMessage()));
        }
    }

    @PostMapping("/answer/accept")
    public ResponseEntity<?> acceptAnswer(@RequestBody AcceptAnswerRequest request) {
        logger.info("Received request to accept answer. Session ID: {}, Question ID: {}, Answer ID: {}, Evaluation ID: {}",
                request.getSessionId(), request.getQuestionId(), request.getAnswerId(), request.getEvaluationId());

        try {
            Optional<Question> questionOpt = questionRepository.findById(request.getQuestionId());
            if (questionOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Question not found with ID: " + request.getQuestionId()));
            }
            Question fetchedQuestion = questionOpt.get();

            Optional<Answer> answerOpt = answerRepository.findById(request.getAnswerId());
            if (answerOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Answer not found with ID: " + request.getAnswerId()));
            }
            // Answer fetchedAnswer = answerOpt.get(); // Not directly used in QASummary for now

            Optional<Evaluation> evalOpt = evaluationRepository.findById(request.getEvaluationId());
            if (evalOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Evaluation not found with ID: " + request.getEvaluationId()));
            }
            // Evaluation fetchedEvaluation = evalOpt.get(); // Not directly used in QASummary for now

            String questionTextHash = hashService.generateSha256Hash(fetchedQuestion.getText());

            QASummary qaSummary = new QASummary();
            qaSummary.setQuestionId(fetchedQuestion.getId());
            qaSummary.setTechnologyStack(fetchedQuestion.getTechnologyStack());
            qaSummary.setExperienceLevel(fetchedQuestion.getExperienceLevel());
            qaSummary.setQuestionTextHash(questionTextHash);
            qaSummary.setCreatedAt(LocalDateTime.now());
            // qaSummary.setId(null); // ID will be set by DB

            qaSummaryRepository.save(qaSummary);
            logger.info("QASummary saved for Question ID: {} with hash: {}", fetchedQuestion.getId(), questionTextHash);

            // MCP context update placeholder
            logger.info("Placeholder: MCP context update for accepted answer (Session: {}) would happen here.", request.getSessionId());

            return ResponseEntity.ok(Map.of("message", "Q&A and summary saved successfully."));

        } catch (Exception e) {
            logger.error("Error processing accept answer request for session ID {}: {}", request.getSessionId(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to process accepted answer: " + e.getMessage()));
        }
    }
}
