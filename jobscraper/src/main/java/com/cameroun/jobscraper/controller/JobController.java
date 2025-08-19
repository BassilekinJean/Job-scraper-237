package com.cameroun.jobscraper.controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cameroun.jobscraper.Dto.JobDto;
import com.cameroun.jobscraper.Dto.PagedResponse;
import com.cameroun.jobscraper.model.JobOffer;
import com.cameroun.jobscraper.scrapper.JobInfoConcoursScraperService;
import com.cameroun.jobscraper.service.JobOfferService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/jobs")
@RequiredArgsConstructor
@Tag(name = "Job", description = "Gestion des offres d'emploi")
public class JobController {

    private final JobOfferService jobOfferService;
    private final JobInfoConcoursScraperService jobInfoConcoursScraperService;

    // GET /jobs?page=0&size=10&location=...
    @GetMapping
    @Operation(summary = "Liste des offres d'emploi", description = "Permet de lister les offres d'emploi avec pagination et filtres")

    public PagedResponse<JobOffer> listJobs(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(required = false) String location,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String company
    ) {
        Page<JobOffer> jobPage = jobOfferService.searchJobs(location, keyword, company, page, size);
        return PagedResponse.of(jobPage);
    }

    // GET /jobs/:id
    @PreAuthorize("hasrole('ADMIN')")
    @GetMapping("/{id}")
    @Operation(summary = "Récupération d'une offre d'emploi par ID", description = "Permet de récupérer une offre d'emploi en fonction de son ID")
    public ResponseEntity<JobOffer> getJobById(@PathVariable Long id) {
        return jobOfferService.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // POST /jobs (pour admin)
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(path = "/create", consumes = "application/json")
    @Operation(summary = "Création d'une nouvelle offre d'emploi", description = "Permet de créer une nouvelle offre d'emploi")
    public ResponseEntity<?> createJob(@Valid @RequestBody JobDto jobDto) {
        jobOfferService.createJob(jobDto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    // POST /jobs/scrape (pour admin)
    @PostMapping("/scrape")
    @Operation(summary = "Lancement du scraping des offres d'emploi", description = "Permet de lancer le scraping des offres d'emploi")
    public ResponseEntity<?> scrapeJobs() {
        try {
            jobInfoConcoursScraperService.scrapeJobs();
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Scraping terminé avec succès");
            response.put("timestamp", LocalDateTime.now().toString());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Erreur lors du scraping");
            errorResponse.put("details", e.getMessage());
            errorResponse.put("timestamp", LocalDateTime.now().toString());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(errorResponse);
        }
    }
}