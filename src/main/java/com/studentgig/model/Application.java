package com.studentgig.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "applications",
    indexes = {
        @Index(name = "idx_app_user", columnList = "user_id"),
        @Index(name = "idx_app_job", columnList = "job_id")
    }
)
public class Application {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "user_id", nullable = false)
    public Long userId;

    @Column(name = "job_id", nullable = false)
    public Long jobId;

    @Column(nullable = false, length = 30)
    public String status = "APPLIED";

    @Column(length = 5000)
    public String coverLetter;

    @Column(name = "resume_path")
    public String resumePath;

    @Column(name = "applied_at", nullable = false)
    public LocalDateTime appliedAt = LocalDateTime.now();
}
