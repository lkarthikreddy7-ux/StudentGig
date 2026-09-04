package com.studentgig.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name="jobs", indexes={@Index(name="idx_job_type",columnList="type"),@Index(name="idx_job_location",columnList="location"),@Index(name="idx_job_recruiter",columnList="recruiterId")})
public class Job {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
    public Long recruiterId;
    public Long companyId;
    public String title,company,type,workMode,location,skills,experienceLevel,education,salary;
    public LocalDate deadline;
    public boolean featured;
    @Column(length=5000) public String description;
    public LocalDate createdAt=LocalDate.now();
}
