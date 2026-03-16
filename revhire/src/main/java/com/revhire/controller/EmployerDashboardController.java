package com.revhire.controller;

import com.revhire.model.Job;
import com.revhire.model.User;
import com.revhire.model.Application;
import com.revhire.model.Company;
import com.revhire.repository.JobRepository;
import com.revhire.repository.UserRepository;
import com.revhire.repository.ApplicationRepository;
import com.revhire.repository.CompanyRepository;
import com.revhire.service.ApplicationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/employer")
public class EmployerDashboardController {

    @Autowired
    private JobRepository jobRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ApplicationRepository applicationRepository;
    
    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private ApplicationService applicationService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User employer = userRepository.findByEmail(email).orElse(null);
        
        if (employer == null || !"EMPLOYER".equals(employer.getUserType())) {
            return "redirect:/auth/login";
        }
        
        // Get all jobs for this employer
        List<Job> jobs = jobRepository.findByEmployerId(employer.getId());
        
        // Calculate statistics
        long totalJobs = jobs.size();
        long activeJobs = jobs.stream().filter(j -> "ACTIVE".equals(j.getStatus())).count();
        long totalApplications = jobs.stream().mapToLong(Job::getApplicationCount).sum();
        
        // Get pending reviews (applications with APPLIED status)
        long pendingReviews = 0;
        for (Job job : jobs) {
            pendingReviews += applicationRepository.findByJobIdAndStatus(job.getId(), "APPLIED").size();
        }
        
        model.addAttribute("totalJobs", totalJobs);
        model.addAttribute("activeJobs", activeJobs);
        model.addAttribute("totalApplications", totalApplications);
        model.addAttribute("pendingReviews", pendingReviews);
        model.addAttribute("recentJobs", jobs.stream().limit(5).collect(Collectors.toList()));
        model.addAttribute("user", employer);
        
        return "employer/dashboard";
    }

    @GetMapping("/jobs")
    public String manageJobs(Model model) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User employer = userRepository.findByEmail(email).orElse(null);
        
        if (employer == null) {
            return "redirect:/auth/login";
        }
        
        List<Job> jobs = jobRepository.findByEmployerId(employer.getId());
        model.addAttribute("jobs", jobs);
        
        return "employer/jobs";
    }

    @GetMapping("/job/{id}/applications")
    public String viewJobApplications(@PathVariable Long id,
                                      @RequestParam(required = false, defaultValue = "ALL") String status,
                                      @RequestParam(required = false) String experience,
                                      @RequestParam(required = false) String skills,
                                      @RequestParam(required = false) String education,
                                      @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(pattern = "yyyy-MM-dd") java.time.LocalDate fromDate,
                                      @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(pattern = "yyyy-MM-dd") java.time.LocalDate toDate,
                                      Model model) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User employer = userRepository.findByEmail(email).orElse(null);
        if (employer == null || !"EMPLOYER".equals(employer.getUserType())) {
            return "redirect:/auth/login";
        }

        Job job = jobRepository.findById(id).orElse(null);
        if (job == null) {
            return "redirect:/employer/jobs";
        }

        if (job.getEmployer() == null || !employer.getId().equals(job.getEmployer().getId())) {
            return "redirect:/employer/jobs";
        }
        
        List<Application> allApplications = applicationRepository.findByJobId(id);
        java.util.stream.Stream<Application> stream = allApplications.stream();
        
        if (status != null && !"ALL".equalsIgnoreCase(status)) {
            stream = stream.filter(a -> status.equalsIgnoreCase(a.getStatus()));
        }
        
        if (experience != null && !experience.trim().isEmpty()) {
            final String expLower = experience.toLowerCase();
            stream = stream.filter(a -> a.getUser().getExperience() != null && 
                a.getUser().getExperience().stream().anyMatch(e -> 
                    (e.getJobTitle() != null && e.getJobTitle().toLowerCase().contains(expLower)) ||
                    (e.getCompany() != null && e.getCompany().toLowerCase().contains(expLower))
                ));
        }
        
        if (skills != null && !skills.trim().isEmpty()) {
            final String skillsLower = skills.toLowerCase();
            stream = stream.filter(a -> a.getUser().getSkills() != null && 
                a.getUser().getSkills().toLowerCase().contains(skillsLower));
        }
        
        if (education != null && !education.trim().isEmpty()) {
            final String eduLower = education.toLowerCase();
            stream = stream.filter(a -> a.getUser().getEducation() != null && 
                a.getUser().getEducation().stream().anyMatch(e -> 
                    (e.getDegree() != null && e.getDegree().toLowerCase().contains(eduLower)) ||
                    (e.getInstitution() != null && e.getInstitution().toLowerCase().contains(eduLower))
                ));
        }
        
        if (fromDate != null) {
            stream = stream.filter(a -> a.getApplicationDate() != null && 
                !a.getApplicationDate().toLocalDate().isBefore(fromDate));
        }
        
        if (toDate != null) {
            stream = stream.filter(a -> a.getApplicationDate() != null && 
                !a.getApplicationDate().toLocalDate().isAfter(toDate));
        }
        
        List<Application> applications = stream.collect(Collectors.toList());
        
        // Group by status
        Map<String, List<Application>> groupedApps = new HashMap<>();
        groupedApps.put("APPLIED", allApplications.stream().filter(a -> "APPLIED".equals(a.getStatus())).collect(Collectors.toList()));
        groupedApps.put("UNDER_REVIEW", allApplications.stream().filter(a -> "UNDER_REVIEW".equals(a.getStatus())).collect(Collectors.toList()));
        groupedApps.put("SHORTLISTED", allApplications.stream().filter(a -> "SHORTLISTED".equals(a.getStatus())).collect(Collectors.toList()));
        groupedApps.put("REJECTED", allApplications.stream().filter(a -> "REJECTED".equals(a.getStatus())).collect(Collectors.toList()));
        
        model.addAttribute("job", job);
        model.addAttribute("applications", applications);
        model.addAttribute("groupedApplications", groupedApps);
        model.addAttribute("totalApplications", allApplications.size());
        
        return "employer/applications";
    }

    @PostMapping("/application/{id}/status")
    public String updateApplicationStatus(@PathVariable Long id,
                                         @RequestParam String status,
                                         @RequestParam(required = false) String notes,
                                         RedirectAttributes redirectAttributes) {
        
        try {
            applicationService.updateApplicationStatus(id, status, notes);
            redirectAttributes.addFlashAttribute("success", "Application status updated to " + status);
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        
        Application application = applicationRepository.findById(id).orElse(null);
        if (application != null) {
            return "redirect:/employer/job/" + application.getJob().getId() + "/applications";
        }
        return "redirect:/employer/jobs";
    }

    @PostMapping("/applications/bulk-update")
    public String bulkUpdateApplications(@RequestParam List<Long> applicationIds,
                                        @RequestParam String status,
                                        @RequestParam(required = false) String notes,
                                        RedirectAttributes redirectAttributes) {
        
        int updatedCount = 0;
        for (Long id : applicationIds) {
            try {
                applicationService.updateApplicationStatus(id, status, notes);
                updatedCount++;
            } catch (RuntimeException e) {
                // Skip if error (e.g. withdrawn)
            }
        }
        
        if (updatedCount < applicationIds.size()) {
            redirectAttributes.addFlashAttribute("warning", updatedCount + " applications updated. " + (applicationIds.size() - updatedCount) + " withdrawn applications were skipped.");
        } else {
            redirectAttributes.addFlashAttribute("success", updatedCount + " applications updated to " + status);
        }
        return "redirect:/employer/jobs";
    }

    @GetMapping("/company/edit")
    public String editCompanyProfile(Model model) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User employer = userRepository.findByEmail(email).orElse(null);
        
        if (employer == null) {
            return "redirect:/auth/login";
        }
        
        Company company = companyRepository.findByUserId(employer.getId()).orElse(new Company());
        model.addAttribute("company", company);
        
        return "employer/company-edit";
    }

    @PostMapping("/company/update")
    public String updateCompanyProfile(@ModelAttribute Company company,
                                      RedirectAttributes redirectAttributes) {
        
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User employer = userRepository.findByEmail(email).orElse(null);
        
        if (employer == null) {
            return "redirect:/auth/login";
        }
        
        Company existingCompany = companyRepository.findByUserId(employer.getId()).orElse(new Company());
        existingCompany.setName(company.getName());
        existingCompany.setIndustry(company.getIndustry());
        existingCompany.setCompanySize(company.getCompanySize());
        existingCompany.setLocation(company.getLocation());
        existingCompany.setDescription(company.getDescription());
        existingCompany.setWebsite(company.getWebsite());
        existingCompany.setUser(employer);
        
        companyRepository.save(existingCompany);
        redirectAttributes.addFlashAttribute("success", "Company profile updated successfully!");
        
        return "redirect:/employer/dashboard";
    }
}
