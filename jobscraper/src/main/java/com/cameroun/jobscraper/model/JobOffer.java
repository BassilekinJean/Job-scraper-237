package com.cameroun.jobscraper.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.cameroun.jobscraper.enums.JobSource;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

@Entity
@Data
public class JobOffer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    private String company;

    private String location;

    private String description;

    @Enumerated(EnumType.STRING)
    private JobSource source;

    @Column(columnDefinition = "TEXT", unique = true)
    private String originalUrl;

    private LocalDate applicationDeadline;

    @Column(updatable = false)
    private LocalDateTime postedAt;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
