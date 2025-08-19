package com.cameroun.jobscraper.Dto;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.validation.constraints.NotBlank;


public record JobDto(

    @NotBlank(message = "Le titre ne peut pas être vide")
    String title,

    String company,

    String location,

    @Column(columnDefinition = "TEXT")
    String description,

    @NotBlank(message = "L'url ne peut pas être vide")
    String originalUrl,

    LocalDateTime applicationDeadline

) {

}
