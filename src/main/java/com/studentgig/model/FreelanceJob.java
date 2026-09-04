package com.studentgig.model; import jakarta.persistence.*; import java.time.LocalDate;
@Entity @Table(name="freelance_jobs") public class FreelanceJob { @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id; public Long clientId; public String title,skills,budget,status="OPEN"; @Column(length=4000) public String description; public LocalDate deadline; }
