package com.revhire.controller;

import com.revhire.model.User;
import com.revhire.repository.UserRepository;
import com.revhire.repository.ApplicationRepository;
import com.revhire.repository.SavedJobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/")
    public String home() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        boolean isLoggedIn = !username.equals("anonymousUser");
        
        if (isLoggedIn) {
            return "redirect:/dashboard";
        }
        return "index";
    }

    @GetMapping("/home")
    public String publicHome() {
        return "index";
    }
    
    @GetMapping("/dashboard")
    public String dashboard() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email).orElse(null);
        
        if (user == null) {
            return "redirect:/auth/login";
        }
        
        // Redirect based on user role
        if ("EMPLOYER".equals(user.getUserType())) {
            return "redirect:/employer/dashboard";
        } else {
            return "redirect:/jobseeker/dashboard";
        }
    }
    
    @Autowired
    private com.revhire.service.JobService jobService;
    
    @Autowired
    private com.revhire.repository.CompanyRepository companyRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private SavedJobRepository savedJobRepository;

    @GetMapping("/jobseeker/dashboard")
    public String jobSeekerDashboard(Model model) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email).orElse(null);
        if (user != null) {
            model.addAttribute("user", user);
            
            // Get Recommended Jobs for this user based on skills
            java.util.List<com.revhire.model.Job> recommendedJobs = jobService.getRecommendedJobs(user);
            model.addAttribute("recommendedJobs", recommendedJobs);
            
            // Generate companyNames map for the recommended jobs to render accurately
            java.util.Map<Long, String> companyNames = new java.util.HashMap<>();
            if (recommendedJobs != null) {
                for (com.revhire.model.Job job : recommendedJobs) {
                    if (job.getEmployer() != null && !companyNames.containsKey(job.getEmployer().getId())) {
                        companyRepository.findByUserId(job.getEmployer().getId())
                            .ifPresent(company -> companyNames.put(job.getEmployer().getId(), company.getName()));
                    }
                }
            }
            model.addAttribute("companyNames", companyNames);
            
            // Add Statistics for the Dashboard using direct count methods
            long applicationCount = applicationRepository.countByUserId(user.getId());
            long savedJobsCount = savedJobRepository.countByUserId(user.getId());
            
            model.addAttribute("applicationCount", applicationCount);
            model.addAttribute("savedJobsCount", savedJobsCount);
            
            // Populate applied jobs with statuses for UI
            java.util.List<com.revhire.model.Application> applications = applicationRepository.findByUserId(user.getId());
            java.util.Map<Long, String> applicationStatuses = applications.stream()
                    .collect(java.util.stream.Collectors.toMap(
                        app -> app.getJob().getId(),
                        app -> app.getStatus(),
                        (existing, replacement) -> existing // Guard against duplicates if any
                    ));
            model.addAttribute("applicationStatuses", applicationStatuses);
        } else {
            // Fallback for safety
            model.addAttribute("applicationCount", 0L);
            model.addAttribute("savedJobsCount", 0L);
        }
        return "jobseeker/dashboard";
    }
}
