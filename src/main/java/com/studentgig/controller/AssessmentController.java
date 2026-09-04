package com.studentgig.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studentgig.model.*;
import com.studentgig.repository.*;
import com.studentgig.service.CodeExecutionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/assessments")
public class AssessmentController {
    private final AssessmentRepository repo;
    private final AssessmentAttemptRepository attempts;
    private final UserRepository users;
    private final ObjectMapper mapper = new ObjectMapper();
    private final CodeExecutionService executor;

    public AssessmentController(AssessmentRepository r, AssessmentAttemptRepository a, CodeExecutionService e, UserRepository u) { repo=r; attempts=a; executor=e; users=u; }
    private Long uid(HttpSession s) { Object x=s.getAttribute("userId"); return x instanceof Long ? (Long)x : null; }
    private boolean admin(HttpSession s) { return "ADMIN".equals(String.valueOf(s.getAttribute("role"))); }

    @GetMapping
    public Object list(HttpSession s) {
        if (admin(s)) return Map.of("error", "Administrators use the Admin Control assessment library.");
        return repo.findByActiveTrueOrderByCreatedAtDesc();
    }

    @GetMapping("/all-admin")
    public Object allAdmin(HttpSession s) { return admin(s) ? repo.findAll() : Map.of("error","Admin access required"); }

    @GetMapping("/{id}")
    public Object get(@PathVariable Long id, HttpSession s) {
        Assessment a=repo.findById(id).orElseThrow();
        if (admin(s) || Boolean.TRUE.equals(a.active)) return a;
        return Map.of("error","Assessment is not published");
    }

    @PostMapping("/admin")
    public Object create(@RequestBody Assessment a,HttpSession s) {
        if(!admin(s)) return Map.of("error","Admin access required");
        validate(a); a.id=null; a.durationMinutes=positive(a.durationMinutes,30); a.cheatLimit=positive(a.cheatLimit,3); return repo.save(a);
    }

    @PutMapping("/admin/{id}")
    public Object update(@PathVariable Long id,@RequestBody Assessment a,HttpSession s) {
        if(!admin(s)) return Map.of("error","Admin access required");
        Assessment x=repo.findById(id).orElseThrow();
        x.title=a.title; x.description=a.description; x.category=a.category; x.durationMinutes=positive(a.durationMinutes,30); x.cheatLimit=positive(a.cheatLimit,3); x.active=a.active;
        x.requireCamera=a.requireCamera; x.detectTabSwitch=a.detectTabSwitch; x.detectWindowBlur=a.detectWindowBlur; x.requireFullscreen=a.requireFullscreen; x.blockMobile=a.blockMobile; x.questionsJson=a.questionsJson;
        validate(x); return repo.save(x);
    }

    @DeleteMapping("/admin/{id}")
    public Object delete(@PathVariable Long id,HttpSession s){ if(!admin(s))return Map.of("error","Admin access required"); repo.deleteById(id); return Map.of("message","Deleted"); }

    @PostMapping("/{id}/attempt")
    public Object attempt(@PathVariable Long id,@RequestBody Map<String,Object> body,HttpSession s) {
        Long user=uid(s); if(user==null)return Map.of("error","Please login");
        boolean adminPreview=admin(s);
        Assessment a=repo.findById(id).orElseThrow();
        if(!a.active)return Map.of("error","Assessment is not published");
        try {
            List<Map<String,Object>> qs=mapper.readValue(a.questionsJson==null?"[]":a.questionsJson,new TypeReference<List<Map<String,Object>>>(){});
            Map<String,Object> ans=toMap(body.getOrDefault("answers",Map.of()));
            int score=0,total=0;
            for(int i=0;i<qs.size();i++) {
                Map<String,Object> q=qs.get(i); String type=String.valueOf(q.getOrDefault("type","MCQ")).toUpperCase(Locale.ROOT);
                if(type.equals("MCQ")) {
                    total++; Object correct=q.get("answer"),given=ans.get(String.valueOf(i));
                    if(correct!=null&&given!=null&&String.valueOf(correct).trim().equalsIgnoreCase(String.valueOf(given).trim())) score++;
                } else if(type.equals("CODING")) {
                    total++; String code=String.valueOf(ans.getOrDefault(String.valueOf(i),""));
                    List<Map<String,Object>> tests=toList(q.get("hiddenTests"));
                    if(tests.isEmpty()) tests=toList(q.get("tests"));
                    if(tests.isEmpty() && q.get("sampleInput")!=null) tests=List.of(Map.of("input",String.valueOf(q.get("sampleInput")),"output",String.valueOf(q.getOrDefault("sampleOutput",""))));
                    boolean passed=!code.isBlank() && !tests.isEmpty();
                    for(Map<String,Object> t:tests) {
                        var r=executor.execute(String.valueOf(q.getOrDefault("language","java")),code,String.valueOf(t.getOrDefault("input","")));
                        if(!r.success() || !executor.sameOutput(String.valueOf(t.getOrDefault("output","")),r.output())) { passed=false; break; }
                    }
                    if(passed) score++;
                }
            }
            int violations=Math.max(0,((Number)body.getOrDefault("violations",0)).intValue());
            int limit=positive(a.cheatLimit,3);
            AssessmentAttempt at=new AssessmentAttempt(); at.assessmentId=id; at.userId=user; at.score=score; at.total=total; at.violations=violations; at.autoSubmitted=violations>=limit;
            at.answersJson=mapper.writeValueAsString(ans); at.submittedAt=LocalDateTime.now();
            if(adminPreview) return at;
            return attempts.save(at);
        }catch(Exception e){ return Map.of("error","Could not submit assessment: "+e.getMessage()); }
    }

    @GetMapping("/my-attempts")
    public Object mine(HttpSession s){
        Long user=uid(s);
        return user==null?List.of():attempts.findByUserIdOrderBySubmittedAtDesc(user);
    }

    @GetMapping("/all-attempts-admin")
    public Object allAttemptsAdmin(HttpSession s){
        if(!admin(s)) return Map.of("error","Admin access required");
        List<Map<String,Object>> out=new ArrayList<>();
        for(AssessmentAttempt at:attempts.findAllByOrderBySubmittedAtDesc()){
            Map<String,Object> row=new LinkedHashMap<>();
            row.put("attempt",at);
            repo.findById(at.assessmentId).ifPresent(a->{row.put("assessmentTitle",a.title);row.put("category",a.category);});
            users.findById(at.userId).ifPresent(u->{row.put("studentName",u.name);row.put("studentEmail",u.email);});
            out.add(row);
        }
        return out;
    }

    private void validate(Assessment a){
        if(a.title==null||a.title.isBlank()) throw new IllegalArgumentException("Assessment title is required");
        if(a.questionsJson==null||a.questionsJson.isBlank())a.questionsJson="[]";
        try{ mapper.readTree(a.questionsJson); }catch(Exception e){ throw new IllegalArgumentException("questionsJson must be valid JSON"); }
    }
    private int positive(Integer x,int d){ return x==null||x<1?d:x; }
    @SuppressWarnings("unchecked") private Map<String,Object> toMap(Object x){ if(x instanceof Map<?,?> m){Map<String,Object> out=new HashMap<>();m.forEach((k,v)->out.put(String.valueOf(k),v));return out;}return new HashMap<>(); }
    @SuppressWarnings("unchecked") private List<Map<String,Object>> toList(Object x){
        if(!(x instanceof List<?> l)) return new ArrayList<>(); List<Map<String,Object>> out=new ArrayList<>();
        for(Object item:l) if(item instanceof Map<?,?> m){Map<String,Object> v=new HashMap<>();m.forEach((k,val)->v.put(String.valueOf(k),val));out.add(v);} return out;
    }
}
