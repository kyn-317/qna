package com.kyn.qna.repository;

import com.kyn.qna.model.Answer;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AnswerRepository extends MongoRepository<Answer, String> {
    // Basic CRUD operations are inherited
}
