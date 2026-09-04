package com.studentgig.model;
import jakarta.persistence.*; import java.time.LocalDateTime;
@Entity @Table(name="assessments", indexes={@Index(name="idx_assessment_active", columnList="active")})
public class Assessment { @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id; @Column(nullable=false) public String title; public String description; public String category; public Integer durationMinutes=30; public Integer cheatLimit=3; public boolean active=true; public boolean requireCamera=true; public boolean detectTabSwitch=true; public boolean detectWindowBlur=true; public boolean requireFullscreen=false; public boolean blockMobile=false; @Lob @Column(columnDefinition="LONGTEXT") public String questionsJson="[]"; public LocalDateTime createdAt=LocalDateTime.now(); }
