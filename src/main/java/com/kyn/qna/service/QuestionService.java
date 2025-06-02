package com.kyn.qna.service;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.kyn.qna.entity.Question;
import com.kyn.qna.repository.QuestionRepository;
import com.google.genai.types.Tool;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuestionService {
    
    private final QuestionRepository questionRepository;
    
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
    public Mono<List<Question>> getQuestionsByCategory(String category) {
        log.info("Executing: getQuestionsByCategory({})", category);
        return questionRepository.findByCategory(category).collectList();
    }
    
    /**
     * Get question count by category
     */
    public Mono<Long> getQuestionCountByCategory(String category) {
        log.info("Executing: getQuestionCountByCategory({})", category);
        return questionRepository.countByCategory(category);
    }    

} 