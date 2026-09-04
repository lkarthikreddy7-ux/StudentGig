package com.studentgig.controller;

import com.studentgig.model.Job;
import com.studentgig.repository.JobRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/jobs")
public class JobController {
    private final JobRepository repo;
    public JobController(JobRepository repo){this.repo=repo;}
    private boolean logged(HttpSession s){return s.getAttribute("userId")!=null;}
    private Long uid(HttpSession s){Object id=s.getAttribute("userId");return id instanceof Long?(Long)id:null;}
    private String role(HttpSession s){return String.valueOf(s.getAttribute("role"));}
    @GetMapping public Object all(@RequestParam(required=false)String q,@RequestParam(required=false)String type,@RequestParam(required=false)String location,HttpSession s){
        if(!logged(s)) return Map.of("error","Please login first");
        return repo.findAll().stream()
            .filter(j->q==null||q.isBlank()||((String.valueOf(j.title)+" "+String.valueOf(j.company)+" "+String.valueOf(j.skills)).toLowerCase().contains(q.toLowerCase())))
            .filter(j->type==null||type.isBlank()||String.valueOf(j.type).equalsIgnoreCase(type))
            .filter(j->location==null||location.isBlank()||String.valueOf(j.location).toLowerCase().contains(location.toLowerCase()))
            .toList();
    }
    @PostMapping public Object create(@RequestBody Job j,HttpSession s){return Map.of("error","Use the recruiter workspace or admin controls to publish jobs");}
    @GetMapping("/company/{companyId}")
    public Object companyJobs(@PathVariable Long companyId, HttpSession s){
        if(!logged(s)) return Map.of("error","Please login first");
        return repo.findByCompanyId(companyId);
    }

    @GetMapping("/{id}") public Object get(@PathVariable Long id,HttpSession s){if(!logged(s))return Map.of("error","Please login first");return repo.findById(id).orElse(null);}
    @PutMapping("/{id}") public Object update(@PathVariable Long id,@RequestBody Job j,HttpSession s){
        if("ADMIN".equals(role(s))) {j.id=id;return repo.save(j);}
        return Map.of("error","Use the recruiter workspace to manage your jobs");
    }
    @DeleteMapping("/{id}") public Object delete(@PathVariable Long id,HttpSession s){
        if("ADMIN".equals(role(s))){repo.deleteById(id);return Map.of("message","Deleted");}
        return Map.of("error","Use the recruiter workspace to manage your jobs");
    }
}
