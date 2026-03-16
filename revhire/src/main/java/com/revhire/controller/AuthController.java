package com.revhire.controller;

import com.revhire.model.User;
import com.revhire.model.Company;
import com.revhire.service.UserService;
import com.revhire.service.CompanyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private UserService userService;
    
    @Autowired
    private CompanyService companyService;

    // ==================== LOGIN ====================
    
    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "error", required = false) String error,
                           @RequestParam(value = "registered", required = false) String registered,
                           @RequestParam(value = "logout", required = false) String logout,
                           @RequestParam(value = "reset", required = false) String reset,
                           Model model) {
        if (error != null) model.addAttribute("error", "Invalid email or password");
        if (registered != null) model.addAttribute("success", "Registration successful! Please login.");
        if (logout != null) model.addAttribute("success", "You have been logged out successfully.");
        if (reset != null) model.addAttribute("success", "Password reset successful! Please login with your new password.");
        return "auth/login";
    }

    @GetMapping("/register")
    public String showRegistrationChoicePage() {
        return "auth/register-choice";
    }

    // ==================== JOB SEEKER REGISTRATION ====================
    
    @GetMapping("/register/jobseeker")
    public String showJobSeekerRegistrationPage(Model model) {
        model.addAttribute("employmentStatuses", new String[]{"EMPLOYED", "UNEMPLOYED", "STUDENT", "FREELANCER"});
        return "auth/register-jobseeker";
    }

    @PostMapping("/register/jobseeker")
    public String registerJobSeeker(@RequestParam String firstName,
                                   @RequestParam String lastName,
                                   @RequestParam String email,
                                   @RequestParam(required = false) String phone,
                                   @RequestParam String location,
                                   @RequestParam String employmentStatus,
                                   @RequestParam String password,
                                   @RequestParam String confirmPassword,
                                   @RequestParam(value = "termsAccepted", required = false) String termsAccepted,
                                   Model model) {
        try {
            // Validation
            if (!password.equals(confirmPassword)) {
                model.addAttribute("error", "Passwords do not match");
                model.addAttribute("employmentStatuses", new String[]{"EMPLOYED", "UNEMPLOYED", "STUDENT", "FREELANCER"});
                return "auth/register-jobseeker";
            }
            
            if (password.length() < 8) {
                model.addAttribute("error", "Password must be at least 8 characters");
                model.addAttribute("employmentStatuses", new String[]{"EMPLOYED", "UNEMPLOYED", "STUDENT", "FREELANCER"});
                return "auth/register-jobseeker";
            }
            
            if (termsAccepted == null) {
                model.addAttribute("error", "You must accept Terms and Conditions");
                model.addAttribute("employmentStatuses", new String[]{"EMPLOYED", "UNEMPLOYED", "STUDENT", "FREELANCER"});
                return "auth/register-jobseeker";
            }

            // Check if user already exists
            if (userService.findByEmail(email).isPresent()) {
                model.addAttribute("error", "Email already registered");
                model.addAttribute("employmentStatuses", new String[]{"EMPLOYED", "UNEMPLOYED", "STUDENT", "FREELANCER"});
                return "auth/register-jobseeker";
            }

            // Create user
            User user = new User();
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setEmail(email);
            user.setPhone(phone);
            user.setLocation(location);
            user.setEmploymentStatus(employmentStatus);
            user.setPassword(password);
            user.setUserType("JOBSEEKER");
            user.setActive(true);
            
            userService.registerUser(user);
            
            return "redirect:/auth/login?registered=true";
        } catch (Exception e) {
            model.addAttribute("error", "Registration failed: " + e.getMessage());
            model.addAttribute("employmentStatuses", new String[]{"EMPLOYED", "UNEMPLOYED", "STUDENT", "FREELANCER"});
            return "auth/register-jobseeker";
        }
    }

    // ==================== EMPLOYER REGISTRATION ====================
    
    @GetMapping("/register/employer")
    public String showEmployerRegistrationPage(Model model) {
        model.addAttribute("industries", new String[]{
            "Technology", "Healthcare", "Finance", "Education", "Retail", 
            "Manufacturing", "Construction", "Hospitality", "Media", "Transportation", "Other"
        });
        model.addAttribute("companySizes", new String[]{
            "1-10", "11-50", "51-200", "201-500", "501-1000", "1000+"
        });
        return "auth/register-employer";
    }

    @PostMapping("/register/employer")
    public String registerEmployer(@RequestParam String firstName,
                                  @RequestParam String lastName,
                                  @RequestParam String email,
                                  @RequestParam String phone,
                                  @RequestParam String password,
                                  @RequestParam String confirmPassword,
                                  @RequestParam String companyName,
                                  @RequestParam String industry,
                                  @RequestParam String companySize,
                                  @RequestParam String location,
                                  @RequestParam String description,
                                  @RequestParam(required = false) String website,
                                  @RequestParam String position,
                                  @RequestParam(value = "termsAccepted", required = false) String termsAccepted,
                                  Model model) {
        try {
            // Validation
            if (!password.equals(confirmPassword)) {
                model.addAttribute("error", "Passwords do not match");
                model.addAttribute("industries", new String[]{"Technology", "Healthcare", "Finance", "Education", "Retail", "Manufacturing", "Construction", "Hospitality", "Media", "Transportation", "Other"});
                model.addAttribute("companySizes", new String[]{"1-10", "11-50", "51-200", "201-500", "501-1000", "1000+"});
                return "auth/register-employer";
            }
            
            if (password.length() < 8) {
                model.addAttribute("error", "Password must be at least 8 characters");
                model.addAttribute("industries", new String[]{"Technology", "Healthcare", "Finance", "Education", "Retail", "Manufacturing", "Construction", "Hospitality", "Media", "Transportation", "Other"});
                model.addAttribute("companySizes", new String[]{"1-10", "11-50", "51-200", "201-500", "501-1000", "1000+"});
                return "auth/register-employer";
            }
            
            if (termsAccepted == null) {
                model.addAttribute("error", "You must accept Terms and Conditions");
                model.addAttribute("industries", new String[]{"Technology", "Healthcare", "Finance", "Education", "Retail", "Manufacturing", "Construction", "Hospitality", "Media", "Transportation", "Other"});
                model.addAttribute("companySizes", new String[]{"1-10", "11-50", "51-200", "201-500", "501-1000", "1000+"});
                return "auth/register-employer";
            }

            // Check if user already exists
            if (userService.findByEmail(email).isPresent()) {
                model.addAttribute("error", "Email already registered");
                model.addAttribute("industries", new String[]{"Technology", "Healthcare", "Finance", "Education", "Retail", "Manufacturing", "Construction", "Hospitality", "Media", "Transportation", "Other"});
                model.addAttribute("companySizes", new String[]{"1-10", "11-50", "51-200", "201-500", "501-1000", "1000+"});
                return "auth/register-employer";
            }

            // Create employer user
            User user = new User();
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setEmail(email);
            user.setPhone(phone);
            user.setLocation(location);
            user.setPassword(password);
            user.setUserType("EMPLOYER");
            user.setActive(true);
            
            User savedUser = userService.registerUser(user);
            
            // Create company
            Company company = new Company();
            company.setName(companyName);
            company.setIndustry(industry);
            company.setCompanySize(companySize);
            company.setLocation(location);
            company.setDescription(description);
            company.setWebsite(website);
            company.setUser(savedUser);
            
            companyService.createCompany(company);
            
            return "redirect:/auth/login?registered=true";
        } catch (Exception e) {
            model.addAttribute("error", "Registration failed: " + e.getMessage());
            model.addAttribute("industries", new String[]{"Technology", "Healthcare", "Finance", "Education", "Retail", "Manufacturing", "Construction", "Hospitality", "Media", "Transportation", "Other"});
            model.addAttribute("companySizes", new String[]{"1-10", "11-50", "51-200", "201-500", "501-1000", "1000+"});
            return "auth/register-employer";
        }
    }
    
    // ==================== FORGOT PASSWORD ====================
    
    @GetMapping("/forgot-password")
    public String showForgotPasswordPage() {
        return "auth/forgot-password";
    }
    
    @PostMapping("/forgot-password")
    public String processForgotPassword(@RequestParam String email, Model model) {
        try {
            // Check if email exists
            if (!userService.findByEmail(email).isPresent()) {
                model.addAttribute("error", "Email not found in our records");
                return "auth/forgot-password";
            }
            
            // Create reset token and send email
            userService.createPasswordResetTokenForUser(email);
            
            model.addAttribute("success", "Password reset instructions have been sent to your email.");
        } catch (Exception e) {
            model.addAttribute("error", "Failed to process request: " + e.getMessage());
        }
        return "auth/forgot-password";
    }
    
    @GetMapping("/reset-password")
    public String showResetPasswordPage(@RequestParam String token, Model model) {
        String result = userService.validatePasswordResetToken(token);
        
        if (result != null) {
            if (result.equals("invalidToken")) {
                model.addAttribute("error", "Invalid password reset token");
            } else if (result.equals("expired")) {
                model.addAttribute("error", "Password reset token has expired. Please request a new one.");
            }
            return "auth/forgot-password";
        }
        
        model.addAttribute("token", token);
        return "auth/reset-password";
    }
    
    @PostMapping("/reset-password")
    public String processResetPassword(@RequestParam String token,
                                      @RequestParam String password,
                                      @RequestParam String confirmPassword,
                                      RedirectAttributes redirectAttributes) {
        
        if (!password.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "Passwords do not match");
            return "redirect:/auth/reset-password?token=" + token;
        }
        
        if (password.length() < 8) {
            redirectAttributes.addFlashAttribute("error", "Password must be at least 8 characters");
            return "redirect:/auth/reset-password?token=" + token;
        }
        
        try {
            String result = userService.validatePasswordResetToken(token);
            
            if (result != null) {
                if (result.equals("invalidToken")) {
                    redirectAttributes.addFlashAttribute("error", "Invalid password reset token");
                } else if (result.equals("expired")) {
                    redirectAttributes.addFlashAttribute("error", "Password reset token has expired");
                }
                return "redirect:/auth/forgot-password";
            }
            
            userService.changeUserPassword(token, password);
            redirectAttributes.addFlashAttribute("success", "Password reset successful! Please login with your new password.");
            return "redirect:/auth/login?reset=true";
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to reset password: " + e.getMessage());
            return "redirect:/auth/forgot-password";
        }
    }
}
