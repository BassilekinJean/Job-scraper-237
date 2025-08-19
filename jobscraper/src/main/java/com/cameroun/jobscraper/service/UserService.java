package com.cameroun.jobscraper.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.cameroun.jobscraper.Dto.UserRegistrationDto;
import com.cameroun.jobscraper.enums.AuthProvider;
import com.cameroun.jobscraper.enums.Role;
import com.cameroun.jobscraper.model.Utilisateur;
import com.cameroun.jobscraper.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {
private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public void registerNewUser(UserRegistrationDto registrationDto) {

        Utilisateur newUser = new Utilisateur();
        newUser.setUserEmail(registrationDto.userEmail());
        newUser.setUserPassword(passwordEncoder.encode(registrationDto.userPassword()));
        newUser.setProvider(AuthProvider.LOCAL);
        
        // temporairement, on attribue un rôle admin si l'email est
        if ("admin@exemple.com".equals(registrationDto.userEmail())) { 
            newUser.setRole(Role.ROLE_ADMIN); 
        } else {
            newUser.setRole(Role.ROLE_USER);
        }

        userRepository.save(newUser);
    }

    public Utilisateur findUserWithEmail(String email){
         return userRepository.findByUserEmail(email);
    }

}
