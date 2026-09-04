package com.studentgig.model;
import jakarta.persistence.*; import java.time.LocalDateTime;
@Entity @Table(name="assessment_attempts", indexes={@Index(name="idx_attempt_user",columnList="userId"),@Index(name="idx_attempt_assessment",columnList="assessmentId")})
public class AssessmentAttempt { @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id; public Long assessmentId; public Long userId; public Integer score=0; public Integer total=0; public Integer violations=0; public boolean autoSubmitted=false; public LocalDateTime startedAt=LocalDateTime.now(); public LocalDateTime submittedAt; @Lob @Column(columnDefinition="LONGTEXT") public String answersJson="{}"; }
