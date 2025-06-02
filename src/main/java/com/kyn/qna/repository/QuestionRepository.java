package com.kyn.qna.repository;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import com.kyn.qna.entity.Question;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface QuestionRepository extends ReactiveMongoRepository<Question, String> {
    

    Flux<Question> findByCategory(String category);
    
    Flux<Question> findByExpYears(int expYears);
    
    Flux<Question> findByCategoryAndExpYears(String category, int expYears);
    
    Flux<Question> findByScoreBetween(int minScore, int maxScore);
    
    @Query("{ 'question': { $regex: ?0, $options: 'i' } }")
    Flux<Question> findByQuestionContainingIgnoreCase(String keyword);
    
    @Query("{ 'userAnswer': { $regex: ?0, $options: 'i' } }")
    Flux<Question> findByUserAnswerContainingIgnoreCase(String keyword);
    
    
    Mono<Long> countByCategory(String category);
        

} 