package com.cameroun.jobscraper.service;

import java.util.Collections;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.cameroun.jobscraper.model.Utilisateur;
import com.cameroun.jobscraper.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomUserDetailService implements UserDetailsService{

    private final UserRepository userRepository;
    private final LoginAttemptService loginAttemptService; // Inject the new service

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Utilisateur user = userRepository.findByUserEmail(username);

        if (user == null) {
            throw new UsernameNotFoundException("Utilisateur non Trouvé avec Email: " + username);
        }

        if (loginAttemptService.isAccountLocked(username)) {
            throw new UsernameNotFoundException("Votre compte est temporairement bloqué en raison de trop de tentatives de connexion échouées.");
        }

        return new org.springframework.security.core.userdetails.User(
            user.getUserEmail(),
            user.getUserPassword(),
            Collections.singletonList(new SimpleGrantedAuthority(user.getRole().toString()))
        );
    }
}