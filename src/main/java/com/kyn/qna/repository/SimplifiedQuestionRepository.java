package com.kyn.qna.repository;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

import reactor.core.publisher.Flux;

import com.kyn.qna.entity.SimplifiedQuestion;

public interface SimplifiedQuestionRepository extends ReactiveMongoRepository<SimplifiedQuestion, String> {
    Flux<SimplifiedQuestion> findByCategory(String category);
}
