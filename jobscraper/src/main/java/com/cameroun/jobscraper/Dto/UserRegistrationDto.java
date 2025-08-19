package com.cameroun.jobscraper.Dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserRegistrationDto(

   
    @NotBlank(message = "L'email ne peut pas être vide")
    @Email(message = "Format d'email invalide")
    String userEmail,

    
    @NotBlank(message = "Le mot de passe ne peut pas être vide")
    @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères")
    String userPassword,

    @NotBlank(message = "Veuillez confirmer le mot de passe")
    @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères")
    String confirmPassword

) {

}
