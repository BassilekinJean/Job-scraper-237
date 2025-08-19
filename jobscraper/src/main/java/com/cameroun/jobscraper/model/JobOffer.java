package com.cameroun.jobscraper.model;

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
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobOffer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    private String company;

    private String location;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    private JobSource source;

    @Column(columnDefinition = "TEXT", unique = true)
    private String originalUrl;

    private LocalDateTime applicationDeadline;

    @Column(updatable = false)
    private LocalDateTime postedAt;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
