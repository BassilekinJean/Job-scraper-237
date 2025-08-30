package com.cameroun.jobscraper.scrapper;

import java.time.Duration;
import java.time.LocalDateTime;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cameroun.jobscraper.enums.JobSource;
import com.cameroun.jobscraper.model.JobOffer;

import com.cameroun.jobscraper.repository.JobRepository;

import lombok.RequiredArgsConstructor;

@Service 
@RequiredArgsConstructor
public class EmploiCmScrapper {

    private static final Logger logger = LoggerFactory.getLogger(JobInfoConcoursScraper.class);

    private final JobRepository jobOfferRepository;
    
    private final WebDriver webDriver; 

    public void scrapeJobs() {
        String listUrl = "https://www.emploi.cm/recherche-jobs-cameroun";
        int clicksCounter = 0; 
        final int MAX_CLICKS = 5; 

        try  {
            webDriver.get(listUrl);

            while (clicksCounter < MAX_CLICKS) { 
                String renderedHtml = webDriver.getPageSource();
                Document listPage = Jsoup.parse(renderedHtml, "https://www.emploi.cm");

                Elements jobLinks = listPage.select(".card-job-detail > h3").select("a");
                for (Element link : jobLinks) {
                    String jobUrl = link.attr("abs:href"); 
                    scrapeJobDetails(jobUrl);
                    logger.info("Scraping : {}", jobUrl);
                }

                WebDriverWait wait = new WebDriverWait(webDriver, Duration.ofSeconds(10));
                WebElement nextButton = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(".pager-next.active.pagination-next a")));

                if (nextButton == null || !nextButton.isDisplayed()) {
                    logger.info("Fin de la pagination, plus de bouton 'suivant' trouvé.");
                    break; 
                }

                // Scroller vers le bouton
                ((org.openqa.selenium.JavascriptExecutor) webDriver).executeScript("arguments[0].scrollIntoView(true);", nextButton);

                nextButton.click();
                
                clicksCounter++;
            }
            logger.info("Pagination terminée après {} clics.", clicksCounter);

            } catch (TimeoutException e) {
                logger.warn("La page suivante n'a pas chargé dans les délais impartis. Fin de la pagination.", e);
            } catch (Exception e) {
                logger.error("Une erreur s'est produite pendant la pagination", e);
            } finally {
                if (webDriver != null) {
                    webDriver.quit();
                }
            }
    }
    

    // private String getPageSourceWithSelenium(String url) {
    //     try {
    //         webDriver.get(url);
    //         new WebDriverWait(webDriver, Duration.ofSeconds(10)).until(
    //             ExpectedConditions.visibilityOfElementLocated(By.cssSelector("...")));
    //         return webDriver.getPageSource();
    //     } catch (Exception e) {
    //         logger.error("Erreur lors du chargement de la page avec selenium: {}", url, e);
    //         return null;
    //     }
    // }

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

                    String localisation = detailsPage.select(".withicon.location-dot > span").text();

                    Element companyElement = detailsPage.select(".card-block-company > ul > li h3 a").first();
                    String company = companyElement != null ? companyElement.text() : "";
                    System.out.println("Company: " + company);

                    String description = detailsPage.select(".job-description").text();

                    Element timeElement = detailsPage.select(".page-application-details > p").first();

                    LocalDateTime postedAt = convertDateElement(timeElement);

                    JobOffer jobOffer = new JobOffer();
                    jobOffer.setSource(JobSource.EMPLOI_CM);
                    jobOffer.setPostedAt(postedAt);
                    jobOffer.setTitle(title);
                    jobOffer.setCompany(company);
                    jobOffer.setDescription(description);
                    jobOffer.setOriginalUrl(url);
                    jobOffer.setLocation(localisation);
                    jobOffer.setCreatedAt(LocalDateTime.now());

                    jobOfferRepository.save(jobOffer);

                    success = true;
                }

            } catch (Exception e) {
                logger.warn("Tentative {} échouée pour l'URL : {}", attempt, url);
                if (attempt == maxRetries) {
                    logger.error("Erreur définitive lors du scraping de l'offre d'emploi : {}", url, e);
                }
                try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
            }
        }

    }

    private LocalDateTime convertDateElement(Element publishedElement) {
        LocalDateTime postedA = LocalDateTime.now();
        if (publishedElement != null) {
            String publishedText = publishedElement.text();
            // Chercher le motif "Publiée le dd.MM.yyyy"
            java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("Publiée le (\\d{2}\\.\\d{2}\\.\\d{4})")
                .matcher(publishedText);
            if (matcher.find()) {
                String dateStr = matcher.group(1);
                java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy");
                java.time.LocalDate date = java.time.LocalDate.parse(dateStr, formatter);
                postedA = date.atTime(LocalDateTime.now().toLocalTime());
                
                return postedA;
            }
            return null;
        }
        return null;
    }

}