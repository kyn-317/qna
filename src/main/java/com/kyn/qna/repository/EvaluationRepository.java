package com.kyn.qna.repository;

import com.kyn.qna.model.Evaluation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EvaluationRepository extends MongoRepository<Evaluation, String> {
    // Basic CRUD operations are inherited
}
