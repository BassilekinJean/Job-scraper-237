package com.cameroun.jobscraper.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cameroun.jobscraper.model.JobOffer;

public interface JobRepository extends JpaRepository<JobOffer, Long>{

}
