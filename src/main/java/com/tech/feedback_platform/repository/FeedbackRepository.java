package com.tech.feedback_platform.repository;

import com.tech.feedback_platform.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedbackRepository
        extends JpaRepository<Feedback, String> {
}
