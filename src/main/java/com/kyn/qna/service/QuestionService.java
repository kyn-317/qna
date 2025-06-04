package com.kyn.qna.service;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.kyn.qna.dto.QuestionRequest;
import com.kyn.qna.dto.SimplifiedQuestionRequest;
import com.kyn.qna.entity.Question;
import com.kyn.qna.entity.SimplifiedQuestion;
import com.kyn.qna.mapper.EntityDtoMapper;
import com.kyn.qna.repository.QuestionRepository;
import com.kyn.qna.repository.SimplifiedQuestionRepository;
import com.google.genai.types.Tool;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuestionService {
    
    private final QuestionRepository questionRepository;
    private final SimplifiedQuestionRepository simplifiedQuestionRepository;
    
    /**
     * Get all questions
     */
    
    public Mono<List<Question>> getAllQuestions() {
        log.info("Executing: getAllQuestions()");
        return questionRepository.findAll().collectList();
    }
    
    /**
     * Get question by ID
     */
    public Mono<Question> getQuestionById(String id) {
        log.info("Executing: getQuestionById({})", id);
        return questionRepository.findById(id);
    }
    
    /**
     * Search questions by category
     */
    public Flux<Question> getQuestionsByCategory(String category) {
        log.info("Executing: getQuestionsByCategory({})", category);
        return questionRepository.findByCategory(category);
    }
    
    /**
     * Get question count by category
     */
    public Mono<Long> getQuestionCountByCategory(String category) {
        log.info("Executing: getQuestionCountByCategory({})", category);
        return questionRepository.countByCategory(category);
    }    


    public Mono<Question> insert(QuestionRequest questionRequest) {
        var question = EntityDtoMapper.toQuestion(questionRequest);
        question.setCreatedAt(LocalDateTime.now());
        question.setUpdatedAt(LocalDateTime.now());
        return questionRepository.save(question);
    }

    public Mono<Question> update(QuestionRequest questionRequest) {
        return questionRepository.findById(questionRequest._id())
        .flatMap(existingQuestion -> {
            existingQuestion.setQuestion(questionRequest.question());
            existingQuestion.setUserAnswer(questionRequest.userAnswer());
            existingQuestion.setAdditionalQuestions(questionRequest.additionalQuestions());
            existingQuestion.setModelAnswer(questionRequest.modelAnswer());
            existingQuestion.setScore(questionRequest.score());
            existingQuestion.setUpdatedAt(LocalDateTime.now());
            return questionRepository.save(existingQuestion);
        });
    }

    public Mono<SimplifiedQuestion> saveSimplifiedQuestion(SimplifiedQuestionRequest request){
        var simplifiedQuestion = EntityDtoMapper.toSimplifiedQuestion(request);
        simplifiedQuestion.setCreatedAt(LocalDateTime.now());
        simplifiedQuestion.setUpdatedAt(LocalDateTime.now());
        return simplifiedQuestionRepository.save(simplifiedQuestion);
    }

    public Flux<SimplifiedQuestion> getSimplifiedQuestionByCategory(String category){
        return simplifiedQuestionRepository.findByCategory(category);
    }
} 