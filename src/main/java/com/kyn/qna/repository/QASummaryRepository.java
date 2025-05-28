package com.kyn.qna.repository;

import com.kyn.qna.model.QASummary;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QASummaryRepository extends MongoRepository<QASummary, String> {
    boolean existsByQuestionTextHashAndTechnologyStackAndExperienceLevel(
            String questionTextHash,
            String technologyStack,
            String experienceLevel
    );
}
