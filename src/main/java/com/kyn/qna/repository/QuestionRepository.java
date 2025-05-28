package com.kyn.qna.repository;

import com.kyn.qna.model.Question;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QuestionRepository extends MongoRepository<Question, String> {
    // Basic CRUD operations are inherited
}
