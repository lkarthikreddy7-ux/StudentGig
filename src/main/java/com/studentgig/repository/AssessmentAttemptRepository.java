package com.studentgig.repository;

import com.studentgig.model.AssessmentAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AssessmentAttemptRepository extends JpaRepository<AssessmentAttempt,Long>{
    List<AssessmentAttempt> findByUserIdOrderBySubmittedAtDesc(Long userId);
    List<AssessmentAttempt> findAllByOrderBySubmittedAtDesc();
    List<AssessmentAttempt> findByAssessmentIdOrderBySubmittedAtDesc(Long assessmentId);
}
