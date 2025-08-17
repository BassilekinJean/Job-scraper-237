package com.cameroun.jobscraper.service; 

import com.cameroun.jobscraper.model.Utilisateur;
import com.cameroun.jobscraper.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    private final UserRepository userRepository;


    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long LOCK_DURATION_MINUTES = 1;

    public void loginSucceeded(String email) {
        Utilisateur user = userRepository.findByUserEmail(email);
        if (user != null) {
            user.setFailedAttempt(0);
            user.setAccountLocked(false);
            user.setLockTime(null);
            userRepository.save(user);
        }
    }

    public void loginFailed(String email) {
        Utilisateur user = userRepository.findByUserEmail(email);
        if (user != null) {
            user.setFailedAttempt(user.getFailedAttempt() + 1);

            if (user.getFailedAttempt() >= MAX_FAILED_ATTEMPTS) {
                user.setAccountLocked(true);
                user.setLockTime(LocalDateTime.now());
            }
            userRepository.save(user);
        }
    }

    public boolean isAccountLocked(String email) {
        Utilisateur user = userRepository.findByUserEmail(email);
        if (user == null || !user.isAccountLocked()) {
            return false;
        }

        long minutesSinceLock = ChronoUnit.MINUTES.between(user.getLockTime(), LocalDateTime.now());
        if (minutesSinceLock >= LOCK_DURATION_MINUTES) {
            
            user.setAccountLocked(false);
            user.setFailedAttempt(0);
            user.setLockTime(null);
            userRepository.save(user);
            return false; 
        }
        return true;
    }
}