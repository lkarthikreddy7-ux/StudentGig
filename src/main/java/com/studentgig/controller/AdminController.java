package com.studentgig.controller;

import com.studentgig.model.*;
import com.studentgig.repository.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final UserRepository users; private final JobRepository jobs; private final CompanyRepository companies;
    public AdminController(UserRepository u,JobRepository j,CompanyRepository c){users=u;jobs=j;companies=c;}
    private boolean ok(HttpSession s){return "ADMIN".equals(String.valueOf(s.getAttribute("role")));}
    private Object denied(HttpSession s){return ok(s)?null:Map.of("error","Admin access required");}
    @GetMapping("/stats") public Object stats(HttpSession s){Object d=denied(s);return d!=null?d:Map.of("users",users.count(),"jobs",jobs.count(),"companies",companies.count());}
    @GetMapping("/users") public Object allUsers(HttpSession s){Object d=denied(s);if(d!=null)return d;List<User> out=new ArrayList<>();for(User u:users.findAll()){User v=new User();v.id=u.id;v.name=u.name;v.email=u.email;v.role=u.role;v.phone=u.phone;v.location=u.location;v.createdAt=u.createdAt;out.add(v);}return out;}
    @PutMapping("/users/{id}/role") public Object role(@PathVariable Long id,@RequestBody Map<String,String> b,HttpSession s){Object d=denied(s);if(d!=null)return d;User x=users.findById(id).orElseThrow();String r=b.getOrDefault("role","STUDENT").toUpperCase(Locale.ROOT);if(!Set.of("STUDENT","RECRUITER","ADMIN").contains(r))return Map.of("error","Invalid role");Object current=s.getAttribute("userId");if(current instanceof Long && ((Long)current).equals(id) && !r.equals("ADMIN"))return Map.of("error","You cannot remove your own admin role");x.role=r;User saved=users.save(x);saved.password=null;return saved;}
    @GetMapping("/jobs") public Object allJobs(HttpSession s){Object d=denied(s);return d!=null?d:jobs.findAll();}
    @GetMapping("/companies") public Object allCompanies(HttpSession s){Object d=denied(s);return d!=null?d:companies.findAll();}
    @PutMapping("/companies/{id}/verify") public Object verifyCompany(@PathVariable Long id,@RequestBody Map<String,Boolean> body,HttpSession s){Object d=denied(s);if(d!=null)return d;Company c=companies.findById(id).orElseThrow();c.verified=body.getOrDefault("verified",true);return companies.save(c);}
    @DeleteMapping("/users/{id}") public Object deleteUser(@PathVariable Long id,HttpSession s){Object d=denied(s);if(d!=null)return d;Object current=s.getAttribute("userId");if(current instanceof Long&&((Long)current).equals(id))return Map.of("error","You cannot delete your own account");users.deleteById(id);return Map.of("message","User deleted");}
    @DeleteMapping("/jobs/{id}") public Object deleteJob(@PathVariable Long id,HttpSession s){Object d=denied(s);if(d!=null)return d;jobs.deleteById(id);return Map.of("message","Job deleted");}
    @DeleteMapping("/companies/{id}") public Object deleteCompany(@PathVariable Long id,HttpSession s){Object d=denied(s);if(d!=null)return d;companies.deleteById(id);return Map.of("message","Company deleted");}
}
