package com.studentgig.controller;

import com.studentgig.model.Company;
import com.studentgig.repository.CompanyRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/companies")
public class CompanyController {
    private final CompanyRepository repo;
    public CompanyController(CompanyRepository repo){this.repo=repo;}
    private boolean logged(HttpSession s){return s.getAttribute("userId")!=null;}
    private String role(HttpSession s){return String.valueOf(s.getAttribute("role"));}
    private Long uid(HttpSession s){Object id=s.getAttribute("userId");return id instanceof Long?(Long)id:null;}
    @GetMapping public Object all(HttpSession s){return logged(s)?repo.findAll():Map.of("error","Please login first");}
    @PostMapping public Object create(@RequestBody Company c,HttpSession s){
        if(!"RECRUITER".equals(role(s))&&!"ADMIN".equals(role(s)))return Map.of("error","Recruiter or admin access required");
        c.id=null;if("RECRUITER".equals(role(s))){c.ownerId=uid(s);c.verified=false;}return repo.save(c);
    }
    @PutMapping("/{id}") public Object update(@PathVariable Long id,@RequestBody Company c,HttpSession s){
        Company old=repo.findById(id).orElse(null);if(old==null)return Map.of("error","Company not found");
        if("ADMIN".equals(role(s))){c.id=id;return repo.save(c);}
        if(!"RECRUITER".equals(role(s))||!Objects.equals(old.ownerId,uid(s)))return Map.of("error","You do not own this company");
        c.id=id;c.ownerId=old.ownerId;c.verified=old.verified;return repo.save(c);
    }
    @DeleteMapping("/{id}") public Object del(@PathVariable Long id,HttpSession s){
        Company old=repo.findById(id).orElse(null);if(old==null)return Map.of("error","Company not found");
        if(!"ADMIN".equals(role(s))&&(!"RECRUITER".equals(role(s))||!Objects.equals(old.ownerId,uid(s))))return Map.of("error","You do not own this company");
        repo.deleteById(id);return Map.of("message","Deleted");
    }
}
