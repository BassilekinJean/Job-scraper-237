package com.cameroun.jobscraper.controller;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cameroun.jobscraper.model.JobOffer;
import com.cameroun.jobscraper.scrapper.JobInfoConcoursScraperService;
import com.cameroun.jobscraper.service.JobOfferService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobOfferService jobOfferService;
    private final JobInfoConcoursScraperService jobInfoConcoursScraperService;

    // GET /jobs?page=0&size=10&location=...
    @GetMapping
    public Page<JobOffer> listJobs(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(required = false) String location,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String company
    ) {
        return jobOfferService.searchJobs(location, keyword, company, page, size);
    }

    // GET /jobs/:id
    @GetMapping("/{id}")
    public ResponseEntity<JobOffer> getJobById(@PathVariable Long id) {
        return jobOfferService.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // POST /jobs (pour admin)
    @PostMapping
    public JobOffer createJob(@RequestBody JobOffer jobOffer) {
        // Logique pour sauvegarder l'offre manuellement
        return null;
    }

    // POST /jobs/scrape (pour admin)
    @PostMapping("/scrape")
    public void scrapeJobs() {
        jobInfoConcoursScraperService.scrapeJobs();
    }
}