package com.studentgig.config;

import com.studentgig.model.*;
import com.studentgig.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.*;

@Configuration
public class AdminInitializer {
 @Bean CommandLineRunner seed(UserRepository users, JobRepository jobs, CompanyRepository companies, AssessmentRepository assessments, PasswordEncoder enc){ return args -> {
   String email=System.getenv().getOrDefault("STUDENTGIG_ADMIN_EMAIL","admin@studentgig.local");
   String pass=System.getenv().getOrDefault("STUDENTGIG_ADMIN_PASSWORD","Admin@12345");
   if(users.findByEmail(email).isEmpty()){ User u=new User(); u.name="StudentGig Administrator";u.email=email;u.password=enc.encode(pass);u.role="ADMIN";u.bio="Platform administrator";users.save(u); }

   Map<String,Company> cmap=new LinkedHashMap<>();
   String[][] cs={
     {"TechNova Solutions","Software Development","Bengaluru, India"},
     {"PixelWorks Studio","Digital Products","Hyderabad, India"},
     {"DataSpring Analytics","Data & Analytics","Pune, India"},
     {"CloudBridge Systems","Cloud & IT","Chennai, India"},
     {"DesignForge","Design & UX","Mumbai, India"},
     {"CodeOrbit Technologies","Technology","Bengaluru, India"},
     {"QualityLabs","Software Testing","Pune, India"},
     {"GrowthLoop Media","Marketing","Remote / India"},
     {"NextPath Operations","Operations","Delhi, India"},
     {"HelpDeskPro","Customer Support","Noida, India"}
   };
   for(String[] x:cs){
      Company c=companies.findAll().stream().filter(z->x[0].equalsIgnoreCase(z.name)).findFirst().orElse(null);
      if(c==null){c=new Company();c.name=x[0];c.industry=x[1];c.location=x[2];c.website="https://example.com";c.description="Default StudentGig employer profile.";c.verified=true;c.ownerId=null;c=companies.save(c);}
      cmap.put(c.name,c);
   }

   Object[][] seedJobs={
     {"Java Backend Developer Intern","TechNova Solutions","Internship","Remote","India","Java, Spring Boot, MySQL"},
     {"Spring Boot Trainee","TechNova Solutions","Trainee","Hybrid","Bengaluru, India","Java, Spring Boot, REST API"},
     {"Frontend Developer","PixelWorks Studio","Full-time","On-site","Hyderabad, India","HTML, CSS, JavaScript"},
     {"UI Developer Intern","PixelWorks Studio","Internship","Hybrid","Hyderabad, India","JavaScript, CSS, UI"},
     {"Data Analyst Intern","DataSpring Analytics","Internship","Remote","India","Python, SQL, Excel"},
     {"Junior Data Analyst","DataSpring Analytics","Full-time","Hybrid","Pune, India","Python, SQL, Power BI"},
     {"Cloud Support Associate","CloudBridge Systems","Graduate Program","Hybrid","Chennai, India","Linux, AWS, Networking"},
     {"DevOps Intern","CloudBridge Systems","Internship","Remote","India","AWS, Docker, Linux"},
     {"UI/UX Design Intern","DesignForge","Internship","Hybrid","Mumbai, India","Figma, UX Research, Prototyping"},
     {"Product Designer","DesignForge","Full-time","On-site","Mumbai, India","Figma, UX, Design Systems"},
     {"Java Developer Graduate","CodeOrbit Technologies","Graduate Program","On-site","Bengaluru, India","Java, Spring, SQL"},
     {"Python Developer Intern","CodeOrbit Technologies","Internship","Remote","India","Python, Flask, SQL"},
     {"QA Automation Trainee","QualityLabs","Trainee","On-site","Pune, India","Java, Selenium, SQL"},
     {"Content & Marketing Associate","GrowthLoop Media","Part-time","Remote","India","Content, SEO, Analytics"},
     {"Operations Apprentice","NextPath Operations","Apprenticeship","On-site","Delhi, India","Communication, Excel"},
     {"Product Support Associate","HelpDeskPro","Full-time","Hybrid","Noida, India","Communication, SQL, Troubleshooting"}
   };
   List<Job> existingJobs=jobs.findAll();
   for(Job existing:existingJobs){
      Company matching=cmap.get(existing.company);
      if(matching!=null && existing.companyId==null){existing.companyId=matching.id;jobs.save(existing);}
   }
   for(Object[] x:seedJobs){
      String title=(String)x[0], company=(String)x[1];
      if(jobs.findAll().stream().noneMatch(j->title.equalsIgnoreCase(j.title) && company.equalsIgnoreCase(j.company))){
         Company c=cmap.get(company); Job j=new Job();j.title=title;j.company=company;j.companyId=c==null?null:c.id;j.recruiterId=null;j.type=(String)x[2];j.workMode=(String)x[3];j.location=(String)x[4];j.skills=(String)x[5];j.salary="Competitive";j.experienceLevel="Entry Level";j.education="Any degree / relevant skills";j.description="StudentGig default opportunity. Apply through the student job marketplace.";j.featured=true;jobs.save(j);
      }
   }

   if(assessments.count()==0){
      Assessment a=new Assessment();a.title="Java Fundamentals & Coding Test";a.description="MCQ + coding assessment with proctoring.";a.category="Java";a.durationMinutes=20;a.cheatLimit=3;a.requireCamera=false;a.detectTabSwitch=true;a.detectWindowBlur=true;a.requireFullscreen=false;a.blockMobile=false;a.active=true;
      a.questionsJson="[{\"type\":\"MCQ\",\"question\":\"Which keyword creates an object in Java?\",\"options\":[\"class\",\"new\",\"this\",\"static\"],\"answer\":\"new\"},{\"type\":\"MCQ\",\"question\":\"Which collection stores key-value pairs?\",\"options\":[\"List\",\"Set\",\"Map\",\"Queue\"],\"answer\":\"Map\"},{\"type\":\"CODING\",\"question\":\"Read two integers and print their sum.\",\"language\":\"java\",\"sampleInput\":\"2 3\",\"sampleOutput\":\"5\",\"starterCode\":\"import java.util.*;\npublic class Main { public static void main(String[] args){ Scanner sc=new Scanner(System.in); int a=sc.nextInt(), b=sc.nextInt(); System.out.println(a+b); } }\",\"hiddenTests\":[{\"input\":\"2 3\",\"output\":\"5\"},{\"input\":\"10 -4\",\"output\":\"6\"},{\"input\":\"20 30\",\"output\":\"50\"}]}]";
      assessments.save(a);
   }
 } ;}
}
