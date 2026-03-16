package com.revhire.controller;

import com.revhire.model.User;
import com.revhire.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class QuickTestController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @GetMapping("/create-working-user")
    public String createWorkingUser() {
        try {
            // Delete existing user if any
            userRepository.findByEmail("working@test.com").ifPresent(user -> userRepository.delete(user));
            
            User user = new User();
            user.setFirstName("Working");
            user.setLastName("User");
            user.setEmail("working@test.com");
            user.setPassword(passwordEncoder.encode("password123"));
            user.setLocation("Test City");
            user.setUserType("JOBSEEKER");
            user.setPhone("1234567890");
            user.setActive(true);
            
            userRepository.save(user);
            return "✅ SUCCESS! Login with: working@test.com / password123";
        } catch (Exception e) {
            return "❌ Error: " + e.getMessage();
        }
    }
    
    @GetMapping("/fix-my-user")
    public String fixMyUser() {
        try {
            User user = userRepository.findByEmail("parveensyed16@gmail.com").orElse(null);
            if (user == null) {
                return "User not found. Create new one.";
            }
            // Fix password encoding
            user.setPassword(passwordEncoder.encode("password123"));
            userRepository.save(user);
            return "✅ Fixed! Login with parveensyed16@gmail.com / password123";
        } catch (Exception e) {
            return "❌ Error: " + e.getMessage();
        }
    }
}