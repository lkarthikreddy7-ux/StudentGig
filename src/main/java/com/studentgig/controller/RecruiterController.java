package com.studentgig.controller;

import com.studentgig.model.Company;
import com.studentgig.model.Job;
import com.studentgig.model.Application;
import com.studentgig.repository.CompanyRepository;
import com.studentgig.repository.JobRepository;
import com.studentgig.repository.ApplicationRepository;
import com.studentgig.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/recruiter")
public class RecruiterController {
    private final CompanyRepository companies;
    private final JobRepository jobs;
    private final ApplicationRepository applications;
    private final UserRepository users;

    public RecruiterController(CompanyRepository c, JobRepository j, ApplicationRepository a, UserRepository u){
        companies=c; jobs=j; applications=a; users=u;
    }
    private Long uid(HttpSession s){ Object id=s.getAttribute("userId"); return id instanceof Long?(Long)id:null; }
    private boolean recruiter(HttpSession s){ return "RECRUITER".equals(String.valueOf(s.getAttribute("role"))); }
    private Object deny(HttpSession s){ return recruiter(s)?null:Map.of("error","Recruiter access required"); }

    @GetMapping("/dashboard")
    public Object dashboard(HttpSession s){
        Object d=deny(s); if(d!=null)return d;
        Long id=uid(s); List<Job> myJobs=jobs.findByRecruiterId(id);
        int applicantCount=0; for(Job j:myJobs) applicantCount+=applications.findByJobId(j.id).size();
        return Map.of("companies",companies.findByOwnerId(id),"jobs",myJobs,"applicants",applicantCount);
    }

    @PostMapping("/companies")
    public Object createCompany(@RequestBody Company c,HttpSession s){
        Object d=deny(s); if(d!=null)return d; c.id=null;c.ownerId=uid(s);c.verified=false;return companies.save(c);
    }
    @PutMapping("/companies/{id}")
    public Object updateCompany(@PathVariable Long id,@RequestBody Company c,HttpSession s){
        Object d=deny(s); if(d!=null)return d;
        Company old=companies.findById(id).orElseThrow(); if(!Objects.equals(old.ownerId,uid(s)))return Map.of("error","You do not own this company");
        c.id=id;c.ownerId=old.ownerId;c.verified=old.verified;return companies.save(c);
    }
    @PostMapping("/jobs")
    public Object createJob(@RequestBody Job j,HttpSession s){
        Object d=deny(s); if(d!=null)return d;
        Long id=uid(s); Company c=companyOwned(j.companyId,id); if(c==null)return Map.of("error","Select your company");
        j.id=null;j.recruiterId=id;j.companyId=c.id;j.company=c.name;if(j.createdAt==null)j.createdAt=java.time.LocalDate.now();return jobs.save(j);
    }
    @PutMapping("/jobs/{id}")
    public Object updateJob(@PathVariable Long id,@RequestBody Job j,HttpSession s){
        Object d=deny(s); if(d!=null)return d; Job old=jobs.findById(id).orElseThrow();
        if(!Objects.equals(old.recruiterId,uid(s)))return Map.of("error","You do not own this job");
        Company c=companyOwned(j.companyId,uid(s)); if(c==null)return Map.of("error","Select your company");
        j.id=id;j.recruiterId=old.recruiterId;j.companyId=c.id;j.company=c.name;return jobs.save(j);
    }
    @DeleteMapping("/jobs/{id}")
    public Object deleteJob(@PathVariable Long id,HttpSession s){
        Object d=deny(s); if(d!=null)return d; Job j=jobs.findById(id).orElseThrow();
        if(!Objects.equals(j.recruiterId,uid(s)))return Map.of("error","You do not own this job"); jobs.deleteById(id);return Map.of("message","Job deleted");
    }
    @GetMapping("/jobs/{id}/applications")
    public Object applicants(@PathVariable Long id,HttpSession s){
        Object d=deny(s); if(d!=null)return d; Job j=jobs.findById(id).orElseThrow();
        if(!Objects.equals(j.recruiterId,uid(s)))return Map.of("error","You do not own this job");
        List<Map<String,Object>> out=new ArrayList<>(); for(Application a:applications.findByJobId(id)){
            Map<String,Object> row=new LinkedHashMap<>();row.put("application",a);row.put("student",users.findById(a.userId).map(u->Map.of("id",u.id,"name",u.name,"email",u.email,"skills",String.valueOf(u.skills))).orElse(Map.of()));out.add(row);
        } return out;
    }
    @PutMapping("/applications/{id}/status")
    public Object status(@PathVariable Long id,@RequestBody Map<String,String> body,HttpSession s){
        Object d=deny(s); if(d!=null)return d; Application a=applications.findById(id).orElseThrow(); Job j=jobs.findById(a.jobId).orElseThrow();
        if(!Objects.equals(j.recruiterId,uid(s)))return Map.of("error","You do not own this application");
        String status=body.getOrDefault("status","APPLIED").toUpperCase(Locale.ROOT);
        if(!Set.of("APPLIED","SHORTLISTED","INTERVIEW","SELECTED","REJECTED").contains(status))return Map.of("error","Invalid status");
        a.status=status;return applications.save(a);
    }
    private Company companyOwned(Long companyId,Long ownerId){if(companyId==null)return null;return companies.findById(companyId).filter(c->Objects.equals(c.ownerId,ownerId)).orElse(null);}
}
