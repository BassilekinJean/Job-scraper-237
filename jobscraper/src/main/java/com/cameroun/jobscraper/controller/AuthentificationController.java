package com.cameroun.jobscraper.controller;


import org.springframework.web.bind.annotation.RestController;

import com.cameroun.jobscraper.Dto.UserLoginDto;
import com.cameroun.jobscraper.Dto.UserRegistrationDto;
import com.cameroun.jobscraper.model.Utilisateur;
import com.cameroun.jobscraper.security.JWTutils;
import com.cameroun.jobscraper.service.LoginAttemptService;
import com.cameroun.jobscraper.service.UserService;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;



@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthentificationController {

    private final JWTutils jwtUtils;
    private final LoginAttemptService loginAttemptService; 
    private final UserService registrationService;
    //private final OtpService otpService;
    private final AuthenticationManager authenticationManager;

    private final RateLimiter loginRateLimiter;
    private final RateLimiter registerRateLimiter;


    @PostMapping("/register")
    public ResponseEntity<?> incriptionsUser(@Valid @RequestBody UserRegistrationDto registrationDto) {

        try {
            // --- Appliquer le Rate Limiting for Registration ---
            registerRateLimiter.acquirePermission(); 

            if (!registrationDto.userPassword().equals(registrationDto.confirmPassword())) {
                return ResponseEntity.badRequest().body("Les mots de passe ne correspondent pas.");
            }

            if (registrationService.findUserWithEmail(registrationDto.userEmail()) != null) {
                return ResponseEntity.badRequest().body("Un utilisateur avec cet identifiant existe déjà");
            }

            registrationService.registerNewUser(registrationDto);

            return ResponseEntity.ok("Inscription et profil enregistrés avec succès.");

        } catch (RequestNotPermitted e) {
            log.warn("Tentative d'enregistrement limitée pour l'email: {}", registrationDto.userEmail());
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS) 
                    .body("Trop de tentatives d'enregistrement. Veuillez réessayer plus tard.");
        } catch (Exception e) {
            log.error("Erreur lors de l'enregistrement de l'utilisateur: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Une erreur inattendue est survenue.");
        }
    }
    
    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@Valid @RequestBody UserLoginDto loginDto, HttpServletResponse response) { 
        String userEmail = loginDto.username(); 
        try {       

            loginRateLimiter.acquirePermission();

            if (loginAttemptService.isAccountLocked(userEmail)) {
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .body("Votre compte est temporairement bloqué en raison de trop de tentatives de connexion échouées. Veuillez réessayer plus tard.");
            }

            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(userEmail, loginDto.password())
            );

            if (authentication.isAuthenticated()) {
                loginAttemptService.loginSucceeded(userEmail); 

                String accessToken = jwtUtils.generateToken(userEmail);
                String refreshToken = jwtUtils.generateRefreshToken(userEmail);

                Cookie refreshTokenCookie = new Cookie("refreshToken", refreshToken);
                refreshTokenCookie.setHttpOnly(true);
                refreshTokenCookie.setSecure(true); 
                refreshTokenCookie.setPath("/auth/refresh");
                refreshTokenCookie.setMaxAge((int) (jwtUtils.getRefreshTokenExpiration() / 1000)); 
                response.addCookie(refreshTokenCookie);

                Map<String, String> authData = new HashMap<>();
                authData.put("token", accessToken);
                authData.put("type", "Bearer");
                return ResponseEntity.ok(authData);
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Nom d'utilisateur ou mot de passe incorrect");

        } catch (RequestNotPermitted e) {
            log.warn("Tentative de connexion limitée pour l'email: {}", userEmail);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS) 
                    .body("Trop de tentatives de connexion. Veuillez réessayer plus tard.");

        } catch (AuthenticationException e) { 
            log.warn("Tentative de connexion échouée pour l'utilisateur {}: {}", userEmail, e.getMessage());
            loginAttemptService.loginFailed(userEmail); 
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Nom d'utilisateur ou mot de passe incorrect");

        } catch (Exception e) { 
            log.error("Erreur inattendue lors de la connexion de l'utilisateur {}: {}", userEmail, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Une erreur inattendue est survenue.");
        }
    }    

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        String authHeader = request.getHeader("Authorization");
        String accessToken = null;

        // 1. Blacklister l'access token actuel si présent
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            accessToken = authHeader.substring(7);
            jwtUtils.invalidateToken(accessToken); 
            log.info("Access Token invalidé et ajouté à la blacklist pour déconnexion.");
        }

        // 2. Supprimer le refresh token du cookie
        String refreshTokenFromCookie = null;
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals("refreshToken")) {
                    refreshTokenFromCookie = cookie.getValue();
                    break;
                }
            }
        }
        if (refreshTokenFromCookie != null) {
            jwtUtils.invalidateToken(refreshTokenFromCookie); 
            log.info("Refresh Token invalidé et ajouté à la blacklist pour déconnexion.");
        }

        // 3. Écraser le cookie du refresh token pour le supprimer du navigateur
        Cookie deleteRefreshTokenCookie = new Cookie("refreshToken", null); 
        deleteRefreshTokenCookie.setHttpOnly(true);
        deleteRefreshTokenCookie.setSecure(true); 
        deleteRefreshTokenCookie.setPath("/auth/refresh"); 
        deleteRefreshTokenCookie.setMaxAge(0); 
        response.addCookie(deleteRefreshTokenCookie); 

        log.info("Déconnexion réussie. Jeton(s) invalidé(s) et cookie supprimé.");
        return ResponseEntity.ok("Déconnexion réussie.");
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = null;
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals("refreshToken")) {
                    refreshToken = cookie.getValue();
                    break;
                }
            }
        }

        if (refreshToken == null || !jwtUtils.isTokenExpired(refreshToken)) { 
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Jeton de rafraîchissement invalide ou expiré.");
        }

        try {
            String username = jwtUtils.extractUsername(refreshToken);
            if (username == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Jeton de rafraîchissement invalide.");
            }

            String newAccessToken = jwtUtils.generateToken(username);

            Map<String, String> authData = new HashMap<>();
            authData.put("accessToken", newAccessToken);
            authData.put("type", "Bearer");
            return ResponseEntity.ok(authData);

        } catch (Exception e) {
            log.error("Erreur lors du rafraîchissement du jeton: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Échec du rafraîchissement du jeton.");
        }

    }

    @GetMapping("/oauth2/success")
    public void oauth2Success(HttpServletResponse response, Authentication authentication) throws java.io.IOException {
        Utilisateur user = (Utilisateur) authentication.getPrincipal();
        String jwt = jwtUtils.generateToken(user.getUserEmail());
        response.sendRedirect("/api/v1/oauth2/redirect?token=" + jwt);
    }

    // @PostMapping("/request-otp")
    // public ResponseEntity<?> requestOtp(@Valid @RequestBody OtpRequest otpRegisterDto) {

    //     String email = otpRegisterDto.getUserEmail();

    //     if (email == null|| email.isBlank()) {
    //         return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Veuillez fourni un email");
    //     }
    //     otpService.sendOtp(email);

    //     return ResponseEntity.ok("OTP sent");
    // }
    
    // @PostMapping("/verify-otp")
    // public ResponseEntity<?> verifyOtp(@Valid @RequestBody OtpValidation otpValidation) {
        
    //     String email = otpValidation.getUserEmail();
    //     String otpCode = otpValidation.getOtp();

    //     if (email.isBlank() || email == null) {
    //         return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Email Invalide");

    //     } else if (otpCode.isBlank() || otpCode == null) {
    //         return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Code OTP invalide");

    //     }else if(otpService.isOtpValid(email, otpCode)){
    //         return ResponseEntity.ok("Vérification réussi");
    //     }
    //     return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Code OTP Incorrecte");
    // }
    

}