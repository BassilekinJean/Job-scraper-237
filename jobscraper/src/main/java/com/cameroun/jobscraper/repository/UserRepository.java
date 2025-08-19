package com.cameroun.jobscraper.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.cameroun.jobscraper.model.Utilisateur;

@Repository
public interface UserRepository extends JpaRepository<Utilisateur,Long>{

    Utilisateur findByUserEmail(String email);
}
