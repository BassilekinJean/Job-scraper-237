package com.cameroun.jobscraper.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cameroun.jobscraper.model.Utilisateur;


public interface UserRepository extends JpaRepository<Utilisateur,Long>{

    Utilisateur findByUserEmail(String email);
}
