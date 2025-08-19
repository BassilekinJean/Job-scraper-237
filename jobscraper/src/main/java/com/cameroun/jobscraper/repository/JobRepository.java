package com.cameroun.jobscraper.repository;

import java.util.ArrayList;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.cameroun.jobscraper.model.JobOffer;

@Repository
public interface JobRepository extends JpaRepository<JobOffer, Long>{

    ArrayList<JobOffer> findByOriginalUrl(String url);

    @Query("SELECT j FROM JobOffer j WHERE (:location IS NULL OR j.location = :location) " +
           "AND (:keyword IS NULL OR j.title LIKE %:keyword%) " +
           "AND (:company IS NULL OR j.company = :company)")
    Page<JobOffer> searchJobs(@Param("location") String location,
                               @Param("keyword") String keyword,
                               @Param("company") String company,
                               Pageable pageable);


}
