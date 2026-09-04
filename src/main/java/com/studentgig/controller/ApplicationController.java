package com.studentgig.controller;

import com.studentgig.model.Application;
import com.studentgig.model.Job;
import com.studentgig.repository.ApplicationRepository;
import com.studentgig.repository.JobRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/applications")
public class ApplicationController {
    private final ApplicationRepository repo;
    private final JobRepository jobs;

    public ApplicationController(ApplicationRepository repo, JobRepository jobs) {
        this.repo = repo;
        this.jobs = jobs;
    }

    private Long uid(HttpSession s) {
        Object id = s.getAttribute("userId");
        if (id instanceof Number n) return n.longValue();
        try { return id == null ? null : Long.valueOf(String.valueOf(id)); }
        catch (Exception e) { return null; }
    }

    private boolean student(HttpSession s) {
        return "STUDENT".equalsIgnoreCase(String.valueOf(s.getAttribute("role")));
    }

    private boolean manager(HttpSession s) {
        String r = String.valueOf(s.getAttribute("role"));
        return "ADMIN".equalsIgnoreCase(r) || "RECRUITER".equalsIgnoreCase(r);
    }

    @PostMapping(value = "/with-resume", consumes = "multipart/form-data")
    public ResponseEntity<?> applyWithResume(
            @RequestParam("jobId") Long jobId,
            @RequestPart("resume") MultipartFile resume,
            @RequestParam(value = "coverLetter", required = false) String coverLetter,
            HttpSession s) {
        Long userId = uid(s);
        if (userId == null) return error(HttpStatus.UNAUTHORIZED, "Please login first");
        if (!student(s)) return error(HttpStatus.FORBIDDEN, "Only student accounts can apply for jobs");
        if (jobId == null || jobId <= 0) return error(HttpStatus.BAD_REQUEST, "Invalid job selected");

        Job job = jobs.findById(jobId).orElse(null);
        if (job == null) return error(HttpStatus.NOT_FOUND, "This job no longer exists");

        if (repo.existsByUserIdAndJobId(userId, jobId)) {
            return error(HttpStatus.CONFLICT, "You have already applied for this job");
        }

        if (resume == null || resume.isEmpty()) {
            return error(HttpStatus.BAD_REQUEST, "Please upload your resume in PDF format");
        }
        String original = resume.getOriginalFilename() == null ? "" : resume.getOriginalFilename();
        String lower = original.toLowerCase(Locale.ROOT);
        String contentType = resume.getContentType() == null ? "" : resume.getContentType().toLowerCase(Locale.ROOT);
        if (!lower.endsWith(".pdf") || (!contentType.isBlank() && !contentType.equals("application/pdf") && !contentType.equals("application/octet-stream"))) {
            return error(HttpStatus.BAD_REQUEST, "Only PDF resumes are allowed");
        }
        if (resume.getSize() > 5 * 1024 * 1024L) {
            return error(HttpStatus.BAD_REQUEST, "Resume must be 5 MB or smaller");
        }

        Path uploadDir = Path.of("uploads", "resumes");
        try {
            Files.createDirectories(uploadDir);
            String safeName = original.replaceAll("[^a-zA-Z0-9._-]", "_");
            String fileName = userId + "_" + jobId + "_" + System.currentTimeMillis() + "_" + safeName;
            Path baseDir = uploadDir.toAbsolutePath().normalize();
            Path target = baseDir.resolve(fileName).normalize();
            if (!target.startsWith(baseDir)) {
                return error(HttpStatus.BAD_REQUEST, "Invalid resume file name");
            }
            Files.copy(resume.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            Application a = new Application();
            a.userId = userId;
            a.jobId = jobId;
            a.status = "APPLIED";
            a.coverLetter = coverLetter == null || coverLetter.isBlank()
                    ? "Interested in this opportunity."
                    : coverLetter.trim();
            a.resumePath = "/api/applications/resume/" + fileName;
            a.appliedAt = LocalDateTime.now();

            Application saved = repo.saveAndFlush(a);
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("success", true);
            out.put("message", "Application submitted successfully with your PDF resume");
            out.put("application", saved);
            out.put("jobTitle", job.title == null ? "" : job.title);
            out.put("company", job.company == null ? "" : job.company);
            return ResponseEntity.ok(out);
        } catch (Exception e) {
            return error(HttpStatus.INTERNAL_SERVER_ERROR, "Could not save the resume and application. Please try again.");
        }
    }

    @GetMapping("/resume/{fileName:.+}")
    public ResponseEntity<?> resume(@PathVariable String fileName, HttpSession s) {
        if (uid(s) == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            Path file = Path.of("uploads", "resumes", fileName).normalize();
            Path base = Path.of("uploads", "resumes").toAbsolutePath().normalize();
            if (!file.toAbsolutePath().startsWith(base) || !Files.exists(file)) {
                return ResponseEntity.notFound().build();
            }
            byte[] bytes = Files.readAllBytes(file);
            return ResponseEntity.ok()
                    .header("Content-Type", "application/pdf")
                    .header("Content-Disposition", "inline; filename=\"resume.pdf\"")
                    .body(bytes);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping
    public ResponseEntity<?> apply(@RequestBody(required = false) Map<String, Object> body, HttpSession s) {
        Long userId = uid(s);
        if (userId == null) return error(HttpStatus.UNAUTHORIZED, "Please login first");
        if (!student(s)) return error(HttpStatus.FORBIDDEN, "Only student accounts can apply for jobs");
        if (body == null) return error(HttpStatus.BAD_REQUEST, "Application data is missing");

        Long jobId = toLong(body.get("jobId"));
        if (jobId == null) return error(HttpStatus.BAD_REQUEST, "Job information is missing or invalid");

        Job job = jobs.findById(jobId).orElse(null);
        if (job == null) return error(HttpStatus.NOT_FOUND, "This job no longer exists");

        if (repo.existsByUserIdAndJobId(userId, jobId)) {
            return error(HttpStatus.CONFLICT, "You have already applied for this job");
        }

        try {
            Application a = new Application();
            a.userId = userId;
            a.jobId = jobId;
            a.status = "APPLIED";
            Object cover = body.get("coverLetter");
            a.coverLetter = cover == null || String.valueOf(cover).isBlank()
                    ? "Interested in this opportunity."
                    : String.valueOf(cover).trim();
            Object resume = body.get("resumePath");
            a.resumePath = resume == null ? null : String.valueOf(resume);
            a.appliedAt = LocalDateTime.now();

            Application saved = repo.saveAndFlush(a);

            Map<String, Object> out = new LinkedHashMap<>();
            out.put("success", true);
            out.put("message", "Application submitted successfully");
            out.put("application", saved);
            out.put("jobTitle", job.title == null ? "" : job.title);
            out.put("company", job.company == null ? "" : job.company);
            return ResponseEntity.ok(out);
        } catch (Exception e) {
            // A race can still hit the database unique constraint; convert it to a friendly response.
            if (repo.existsByUserIdAndJobId(userId, jobId)) {
                return error(HttpStatus.CONFLICT, "You have already applied for this job");
            }
            return error(HttpStatus.INTERNAL_SERVER_ERROR, "Could not submit application. Please try again.");
        }
    }

    @GetMapping("/me")
    public Object mine(HttpSession s) {
        Long id = uid(s);
        return id == null ? List.of() : repo.findByUserIdOrderByAppliedAtDesc(id);
    }

    @GetMapping("/job/{id}")
    public Object byJob(@PathVariable Long id, HttpSession s) {
        return manager(s) ? repo.findByJobId(id) : Map.of("error", "Recruiter or admin access required");
    }

    @PutMapping("/{id}/status")
    public Object status(@PathVariable Long id, @RequestBody Map<String, String> b, HttpSession s) {
        if (!manager(s)) return Map.of("error", "Recruiter or admin access required");
        Application a = repo.findById(id).orElse(null);
        if (a == null) return Map.of("error", "Application not found");
        String status = b.getOrDefault("status", "APPLIED").toUpperCase(Locale.ROOT);
        if (!Set.of("APPLIED", "SHORTLISTED", "INTERVIEW", "SELECTED", "REJECTED").contains(status)) {
            return Map.of("error", "Invalid application status");
        }
        a.status = status;
        return repo.save(a);
    }

    private Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number n) return n.longValue();
        try { return Long.valueOf(String.valueOf(value).trim()); }
        catch (Exception e) { return null; }
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("error", message));
    }
}
