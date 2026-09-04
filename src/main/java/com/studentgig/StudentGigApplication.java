package com.studentgig;
import com.studentgig.model.User; import com.studentgig.repository.UserRepository; import org.springframework.boot.*; import org.springframework.boot.autoconfigure.*; import org.springframework.context.annotation.Bean; import org.springframework.security.crypto.password.PasswordEncoder;
@SpringBootApplication public class StudentGigApplication { public static void main(String[] args){SpringApplication.run(StudentGigApplication.class,args);}
 @Bean CommandLineRunner seedAdmin(UserRepository repo, PasswordEncoder enc){return args->{String email=System.getenv().getOrDefault("ADMIN_EMAIL","admin@studentgig.local");String pass=System.getenv().getOrDefault("ADMIN_PASSWORD","Admin@12345");if(repo.findByEmail(email).isEmpty()){User u=new User();u.email=email;u.password=enc.encode(pass);u.name="StudentGig Admin";u.role="ADMIN";repo.save(u);}};}
}
