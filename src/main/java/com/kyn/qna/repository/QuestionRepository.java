package com.kyn.qna.repository;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import com.kyn.qna.model.Question;

@Repository
public interface QuestionRepository extends ReactiveMongoRepository<Question, String> {
    

    Flux<Question> findByCategory(String category);
    
    Flux<Question> findByAuthor(String author);
    
    Flux<Question> findByIsAnswered(boolean isAnswered);
    
    @Query("{ 'title': { $regex: ?0, $options: 'i' } }")
    Flux<Question> findByTitleContainingIgnoreCase(String keyword);
    
    @Query("{ 'content': { $regex: ?0, $options: 'i' } }")
    Flux<Question> findByContentContainingIgnoreCase(String keyword);
    
    Mono<Long> countByCategory(String category);
    
    Mono<Long> countByIsAnswered(boolean isAnswered);
    
    @Query(value = "{}", sort = "{ 'viewCount': -1 }")
    Flux<Question> findTopViewedQuestions();
} 