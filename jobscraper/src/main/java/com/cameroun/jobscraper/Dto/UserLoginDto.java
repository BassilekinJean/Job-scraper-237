package com.cameroun.jobscraper.Dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserLoginDto(


    @NotBlank(message = "L'email ne peut pas être vide")
    @Email(message = "Format d'email invalide")
    String username,

    @NotBlank(message = "Le mot de passe ne peut pas être vide")
    @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères")
    String password

) {

}
