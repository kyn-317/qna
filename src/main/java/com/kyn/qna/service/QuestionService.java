package com.kyn.qna.service;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.kyn.qna.model.Question;
import com.kyn.qna.repository.QuestionRepository;

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
     * Search questions by author
     */
    public Mono<List<Question>> getQuestionsByAuthor(String author) {
        log.info("Executing: getQuestionsByAuthor({})", author);
        return questionRepository.findByAuthor(author).collectList();
    }
    
    /**
     * Search questions by title keyword
     */
    public Mono<List<Question>> searchQuestionsByTitle(String keyword) {
        log.info("Executing: searchQuestionsByTitle({})", keyword);
        return questionRepository.findByTitleContainingIgnoreCase(keyword).collectList();
    }
    
    /**
     * Search questions by content keyword
     */
    public Mono<List<Question>> searchQuestionsByContent(String keyword) {
        log.info("Executing: searchQuestionsByContent({})", keyword);
        return questionRepository.findByContentContainingIgnoreCase(keyword).collectList();
    }
    
    /**
     * Get unanswered questions
     */
    public Mono<List<Question>> getUnansweredQuestions() {
        log.info("Executing: getUnansweredQuestions()");
        return questionRepository.findByIsAnswered(false).collectList();
    }
    
    /**
     * Get answered questions
     */
    public Mono<List<Question>> getAnsweredQuestions() {
        log.info("Executing: getAnsweredQuestions()");
        return questionRepository.findByIsAnswered(true).collectList();
    }
    
    /**
     * Get question count by category
     */
    public Mono<Long> getQuestionCountByCategory(String category) {
        log.info("Executing: getQuestionCountByCategory({})", category);
        return questionRepository.countByCategory(category);
    }
    
    /**
     * Get unanswered question count
     */
    public Mono<Long> getUnansweredQuestionCount() {
        log.info("Executing: getUnansweredQuestionCount()");
        return questionRepository.countByIsAnswered(false);
    }
    
    /**
     * Get top viewed questions
     */
    public Mono<List<Question>> getTopViewedQuestions(int limit) {
        log.info("Executing: getTopViewedQuestions({})", limit);
        return questionRepository.findTopViewedQuestions()
                .take(limit)
                .collectList();
    }
} 