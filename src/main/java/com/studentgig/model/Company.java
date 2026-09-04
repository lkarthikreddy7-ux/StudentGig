package com.studentgig.model;

import jakarta.persistence.*;

@Entity
@Table(name="companies", indexes={@Index(name="idx_company_owner", columnList="ownerId")})
public class Company {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
    @Column(nullable=false) public String name;
    public Long ownerId;
    public String logo, industry, location, website, linkedin;
    @Column(length=3000) public String description;
    public boolean verified;
}
