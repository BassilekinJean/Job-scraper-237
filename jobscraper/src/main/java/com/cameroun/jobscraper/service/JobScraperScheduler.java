package com.cameroun.jobscraper.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.cameroun.jobscraper.scrapper.JobInfoConcoursScraper;

@Service
public class JobScraperScheduler {

    private JobInfoConcoursScraper jobInfoConcoursScraperService;
    // Autre services de scraping...

    @Scheduled(cron = "0 0 1 * * ?") // Exécuter tous les jours à 1h du matin
    public void runScrapers() {
        jobInfoConcoursScraperService.scrapeJobs();
        // Lancer les autres services...
    }
}