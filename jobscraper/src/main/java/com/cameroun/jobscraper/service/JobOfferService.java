package com.cameroun.jobscraper.service;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.cameroun.jobscraper.model.JobOffer;
import com.cameroun.jobscraper.repository.JobRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class JobOfferService {

    private final JobRepository jobRepository; 

    public Optional<JobOffer> findById(Long id) {
        return jobRepository.findById(id);
    }

    public Page<JobOffer> searchJobs(String location, String keyword, String company, int page, int size) {

        PageRequest pageRequest = PageRequest.of(page, size);
        return jobRepository.searchJobs(location, keyword, company, pageRequest);
    }

}
