package com.studentgig.repository;
import com.studentgig.model.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface CompanyRepository extends JpaRepository<Company,Long>{ List<Company> findByOwnerId(Long ownerId); }
