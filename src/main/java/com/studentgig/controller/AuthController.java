package com.studentgig.controller;

import com.studentgig.model.User;
import com.studentgig.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;
import java.util.*;

@RestController @RequestMapping("/api/auth")
public class AuthController {
 private final UserRepository repo; private final PasswordEncoder enc;
 public AuthController(UserRepository r,PasswordEncoder e){repo=r;enc=e;}
 @PostMapping("/register") public Object register(@RequestBody User u){
   if(u.email==null||u.email.isBlank()||u.password==null||u.password.length()<6||u.name==null||u.name.isBlank())return Map.of("error","Name, email and a password of at least 6 characters are required");
   if(repo.findByEmail(u.email).isPresent())return Map.of("error","Email already registered");
   u.password=enc.encode(u.password); u.role="RECRUITER".equalsIgnoreCase(u.role)?"RECRUITER":"STUDENT"; repo.save(u); u.password=null; return u;
 }
 @PostMapping("/login") public Object login(@RequestBody Map<String,String> p,HttpSession s){
   String email=p.getOrDefault("email","").trim(); String password=p.getOrDefault("password","");
   Optional<User> found=repo.findByEmail(email);
   if(found.isEmpty()||!enc.matches(password,found.get().password))return Map.of("error","Invalid email or password");
   User u=found.get(); s.setAttribute("userId",u.id); s.setAttribute("role",u.role); u.password=null; return u;
 }
 @PostMapping("/logout") public Object logout(HttpSession s){s.invalidate();return Map.of("message","Logged out");}
 @GetMapping("/me") public Object me(HttpSession s){Object id=s.getAttribute("userId");if(id==null)return Map.of("loggedIn",false);return repo.findById(id instanceof Number ? ((Number)id).longValue() : -1L).map(u->{u.password=null;return (Object)u;}).orElseGet(()->Map.of("loggedIn",false));}
}
