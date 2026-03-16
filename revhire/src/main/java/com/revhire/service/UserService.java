package com.revhire.service;

import com.revhire.model.User;
import com.revhire.model.PasswordResetToken;
import com.revhire.repository.UserRepository;
import com.revhire.repository.PasswordResetTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;
    
    @Autowired
    private BCryptPasswordEncoder passwordEncoder;
    
    @Autowired
    private EmailService emailService;
    
    // ==================== USER REGISTRATION ====================
    
    public User registerUser(User user) {
        // Check if email already exists
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("Email already registered");
        }
        
        // Encrypt password
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        
        // Set default values
        user.setActive(true);
        
        // Save user
        return userRepository.save(user);
    }
    
    // ==================== USER AUTHENTICATION ====================
    
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }
    
    public boolean validatePassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }
    
    // ==================== PASSWORD RESET ====================
    
    public void createPasswordResetTokenForUser(String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
        
        // Delete any existing token for this user
        passwordResetTokenRepository.findByUser(user)
            .ifPresent(token -> passwordResetTokenRepository.delete(token));
        
        // Create new token
        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = new PasswordResetToken(token, user);
        passwordResetTokenRepository.save(resetToken);
        
        // Send email with reset link
        emailService.sendPasswordResetEmail(user.getEmail(), token);
    }
    
    public String validatePasswordResetToken(String token) {
        Optional<PasswordResetToken> passToken = passwordResetTokenRepository.findByToken(token);
        
        if (!passToken.isPresent()) {
            return "invalidToken";
        }
        
        if (passToken.get().isExpired()) {
            passwordResetTokenRepository.delete(passToken.get());
            return "expired";
        }
        
        return null;
    }
    
    public Optional<User> getUserByPasswordResetToken(String token) {
        return passwordResetTokenRepository.findByToken(token)
            .map(PasswordResetToken::getUser);
    }
    
    public void changeUserPassword(String token, String newPassword) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
            .orElseThrow(() -> new RuntimeException("Invalid token"));
        
        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        
        // Delete the used token
        passwordResetTokenRepository.delete(resetToken);
    }
    
    // ==================== USER PROFILE MANAGEMENT ====================
    
    public User updateUserProfile(User user) {
        user.setUpdatedAt(java.time.LocalDateTime.now());
        return userRepository.save(user);
    }
    
    public void deactivateUser(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        user.setActive(false);
        userRepository.save(user);
    }
    
    public void activateUser(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        user.setActive(true);
        userRepository.save(user);
    }
    
    // ==================== USER UTILITIES ====================
    
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }
    
    public boolean emailExists(String email) {
        return userRepository.existsByEmail(email);
    }
    
    public long countUsers() {
        return userRepository.count();
    }
    
    public long countByUserType(String userType) {
        return userRepository.findAll().stream()
            .filter(user -> user.getUserType().equals(userType))
            .count();
    }
}