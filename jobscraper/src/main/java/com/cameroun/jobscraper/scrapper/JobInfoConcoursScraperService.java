package com.cameroun.jobscraper.scrapper;

import java.io.IOException;
import java.time.LocalDateTime;

import javax.net.ssl.SSLHandshakeException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import com.cameroun.jobscraper.enums.JobSource;
import com.cameroun.jobscraper.model.JobOffer;
import com.cameroun.jobscraper.repository.JobRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class JobInfoConcoursScraperService {

    private static final Logger logger = LoggerFactory.getLogger(JobInfoConcoursScraperService.class);

    private final JobRepository jobOfferRepository;

    public void scrapeJobs() {
        try {
            
            Document listPage = Jsoup.connect("https://infosconcourseducation.com/category/offre-demploiss/").get();
            Elements jobLinks = listPage.select(".td-module-meta-info > h3 > a");

            for (Element link : jobLinks) {
                String jobUrl = link.attr("abs:href"); 

                scrapeJobDetails(jobUrl);
            }

        } catch (HttpStatusException e) {
            e.printStackTrace();
            System.out.println("Erreur HTTP : " + e.getStatusCode());
        } catch (SSLHandshakeException e){
            e.printStackTrace();
            System.out.println("Erreur lors de connexion SSL/TLS, vérifier la connexion internet : " + e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Erreur IO lors de la récupération de la liste des offres : " + e.getMessage());
        }
    }

    private void scrapeJobDetails(String url) {
        int maxRetries = 3;
        int attempt = 0;
        boolean success = false;

        while (attempt < maxRetries && !success) {
            try {
                attempt++;
                if (jobOfferRepository.findByOriginalUrl(url).isEmpty()) {
                    Document detailsPage = Jsoup.connect(url).get();

                    // Extraction des données de la page de détails
                    String title = detailsPage.select("h1").first().text();
                    System.out.println("Titre :" + title);

                    Element companyElm = detailsPage.select(".tdb-block-inner.td-fix-index > p").first();
                    String company = companyElm != null ? companyElm.select("strong, b").text() : "";
                    System.out.println("Nom company :" + company);

                    Elements paragraphs = detailsPage.select(".tdb-block-inner > p");
                    String description = paragraphs.size() > 1 ? paragraphs.get(1).text() : paragraphs.first() != null ? paragraphs.first().text() : "";
                    System.out.println("Description :" + description);

                    Element timeElement = detailsPage.select(".td_block_wrap.tdb_single_date.tdi_71.td-pb-border-top.td_block_template_1.tdb-post-meta .tdb-block-inner.td-fix-index time").first();

                    String createdAtStr = timeElement != null ? timeElement.attr("datetime") : null;
                    LocalDateTime postedAt = null;
                    if (createdAtStr != null && !createdAtStr.isEmpty()) {
                        postedAt = LocalDateTime.parse(createdAtStr.substring(0, 19));
                    }

                    JobOffer jobOffer = new JobOffer();
                    jobOffer.setSource(JobSource.INFO_CONCOURS);
                    jobOffer.setPostedAt(postedAt);
                    jobOffer.setTitle(title);
                    jobOffer.setCompany(company);
                    jobOffer.setDescription(description);
                    jobOffer.setOriginalUrl(url);
                    jobOffer.setCreatedAt(LocalDateTime.now());

                    jobOfferRepository.save(jobOffer);

                    success = true;
                }

            } catch (IOException e) {
                logger.warn("Tentative {} échouée pour l'URL : {}", attempt, url);
                if (attempt == maxRetries) {
                    logger.error("Erreur définitive lors du scraping de l'offre d'emploi : {}", url, e);
                }
                try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
            }
        }
    }
}