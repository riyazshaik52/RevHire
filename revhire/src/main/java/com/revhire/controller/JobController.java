package com.revhire.controller;

import com.revhire.model.Job;
import com.revhire.model.User;
import com.revhire.model.Company;
import com.revhire.repository.UserRepository;
import com.revhire.repository.CompanyRepository;
import com.revhire.repository.ResumeRepository;
import com.revhire.repository.SavedJobRepository;
import com.revhire.repository.ApplicationRepository;
import com.revhire.service.JobService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Controller
@RequestMapping("/jobs")
public class JobController {

    @Autowired
    private JobService jobService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private ResumeRepository resumeRepository;

    @Autowired
    private SavedJobRepository savedJobRepository;
    
    @Autowired
    private ApplicationRepository applicationRepository;

    // ==================== PUBLIC JOB SEARCH ====================
    
    @GetMapping
    public String listJobs(Model model) {
        List<Job> jobs = jobService.getAllActiveJobs();
        model.addAttribute("jobs", jobs);
        model.addAttribute("isEmployer", isCurrentUserEmployer());
        model.addAttribute("isJobSeeker", isCurrentUserJobSeeker());
        
        // Add filter options
        model.addAttribute("employmentTypes", Arrays.asList("FULL_TIME", "PART_TIME", "CONTRACT", "INTERNSHIP", "REMOTE"));
        model.addAttribute("experienceLevels", Arrays.asList("ENTRY", "MID", "SENIOR", "LEAD"));
        
        populateCompanyNames(model, jobs);
        populateSavedJobIds(model);
        populateApplicationStatuses(model);
        
        return "jobs/list";
    }

    @GetMapping("/search")
    public String searchJobs(@RequestParam(required = false) String title,
                            @RequestParam(required = false) String location,
                            @RequestParam(required = false) String employmentType,
                            @RequestParam(required = false) String experienceLevel,
                            @RequestParam(required = false) Double minSalary,
                            @RequestParam(required = false) Double maxSalary,
                            @RequestParam(required = false) String companyName,
                            @RequestParam(required = false) Integer datePostedDays,
                            Model model) {
        LocalDateTime datePosted = null;
        if (datePostedDays != null && datePostedDays > 0) {
            datePosted = LocalDateTime.now().minusDays(datePostedDays);
        }
        
        List<Job> jobs = jobService.searchJobs(title, location, employmentType, 
                                              experienceLevel, minSalary, maxSalary,
                                              companyName, datePosted);
        model.addAttribute("jobs", jobs);
        model.addAttribute("isEmployer", isCurrentUserEmployer());
        model.addAttribute("isJobSeeker", isCurrentUserJobSeeker());
        model.addAttribute("employmentTypes", Arrays.asList("FULL_TIME", "PART_TIME", "CONTRACT", "INTERNSHIP", "REMOTE"));
        model.addAttribute("experienceLevels", Arrays.asList("ENTRY", "MID", "SENIOR", "LEAD"));
        
        populateCompanyNames(model, jobs);
        populateSavedJobIds(model);
        populateApplicationStatuses(model);
        
        return "jobs/list";
    }

    private void populateCompanyNames(Model model, List<Job> jobs) {
        java.util.Map<Long, String> companyNames = new java.util.HashMap<>();
        if (jobs != null) {
            for (Job job : jobs) {
                if (job.getEmployer() != null && !companyNames.containsKey(job.getEmployer().getId())) {
                    companyRepository.findByUserId(job.getEmployer().getId())
                        .ifPresent(company -> companyNames.put(job.getEmployer().getId(), company.getName()));
                }
            }
        }
        model.addAttribute("companyNames", companyNames);
    }

    @GetMapping("/{id}")
    public String viewJob(@PathVariable Long id, Model model) {
        Job job = jobService.getJobById(id);
        jobService.incrementViewCount(id);
        model.addAttribute("job", job);
        
        if (job.getEmployer() != null) {
            Company company = companyRepository.findByUserId(job.getEmployer().getId()).orElse(null);
            model.addAttribute("company", company);
        }
        
        // Add current user's resumes if they are a job seeker
        boolean isSeeker = isCurrentUserJobSeeker();
        model.addAttribute("isJobSeeker", isSeeker);
        model.addAttribute("isEmployer", isCurrentUserEmployer());

        if (isSeeker) {
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            userRepository.findByEmail(email).ifPresent(user -> {
                model.addAttribute("resumes", resumeRepository.findByUserId(user.getId()));
                model.addAttribute("isSaved", savedJobRepository.existsByUserIdAndJobId(user.getId(), id));
                
                // Get the application if it exists to show specific status and notes
                applicationRepository.findByUserId(user.getId()).stream()
                    .filter(app -> app.getJob().getId().equals(id))
                    .findFirst()
                    .ifPresent(app -> {
                        model.addAttribute("hasApplied", true);
                        model.addAttribute("application", app);
                    });
            });
        }
        
        return "jobs/view";
    }

    // ==================== JOB POSTING (EMPLOYER ONLY) ====================
    
    @GetMapping("/post")
    public String showPostJobForm(Model model) {
        // Check if user is employer
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email).orElse(null);
        
        if (user == null || !"EMPLOYER".equals(user.getUserType())) {
            return "redirect:/auth/login";
        }
        
        model.addAttribute("job", new Job());
        model.addAttribute("employmentTypes", Arrays.asList("FULL_TIME", "PART_TIME", "CONTRACT", "INTERNSHIP", "REMOTE"));
        model.addAttribute("experienceLevels", Arrays.asList("ENTRY", "MID", "SENIOR", "LEAD"));
        return "jobs/post";
    }

    @PostMapping("/post")
    public String postJob(@ModelAttribute Job job, RedirectAttributes redirectAttributes) {
        try {
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            User employer = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            if (!"EMPLOYER".equals(employer.getUserType())) {
                throw new RuntimeException("Only employers can post jobs");
            }
            
            jobService.createJob(job, employer);
            redirectAttributes.addFlashAttribute("success", "Job posted successfully!");
            return "redirect:/employer/jobs";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to post job: " + e.getMessage());
            return "redirect:/jobs/post";
        }
    }

    // ==================== EDIT JOB (EMPLOYER ONLY) ====================
    
    @GetMapping("/edit/{id}")
    public String showEditJobForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            // Check if user is employer and owns this job
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            User employer = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            Job job = jobService.getJobById(id);
            
            // Verify that this job belongs to the employer
            if (!job.getEmployer().getId().equals(employer.getId())) {
                throw new RuntimeException("You don't have permission to edit this job");
            }
            
            model.addAttribute("job", job);
            model.addAttribute("employmentTypes", Arrays.asList("FULL_TIME", "PART_TIME", "CONTRACT", "INTERNSHIP", "REMOTE"));
            model.addAttribute("experienceLevels", Arrays.asList("ENTRY", "MID", "SENIOR", "LEAD"));
            return "jobs/edit";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/employer/jobs";
        }
    }

    @PostMapping("/update/{id}")
    public String updateJob(@PathVariable Long id, @ModelAttribute Job job, 
                           RedirectAttributes redirectAttributes) {
        try {
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            User employer = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            Job existingJob = jobService.getJobById(id);
            
            // Verify ownership
            if (!existingJob.getEmployer().getId().equals(employer.getId())) {
                throw new RuntimeException("You don't have permission to update this job");
            }
            
            // Update fields
            existingJob.setTitle(job.getTitle());
            existingJob.setDescription(job.getDescription());
            existingJob.setRequirements(job.getRequirements());
            existingJob.setResponsibilities(job.getResponsibilities());
            existingJob.setLocation(job.getLocation());
            existingJob.setMinSalary(job.getMinSalary());
            existingJob.setMaxSalary(job.getMaxSalary());
            existingJob.setSalaryCurrency(job.getSalaryCurrency());
            existingJob.setEmploymentType(job.getEmploymentType());
            existingJob.setExperienceLevel(job.getExperienceLevel());
            existingJob.setEducationLevel(job.getEducationLevel());
            existingJob.setIndustry(job.getIndustry());
            existingJob.setRequiredSkills(job.getRequiredSkills());
            existingJob.setNumberOfOpenings(job.getNumberOfOpenings());
            existingJob.setApplicationDeadline(job.getApplicationDeadline());
            existingJob.setUpdatedAt(LocalDateTime.now());
            
            jobService.updateJob(existingJob);
            redirectAttributes.addFlashAttribute("success", "Job updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to update job: " + e.getMessage());
        }
        return "redirect:/employer/jobs";
    }

    // ==================== MANAGE JOB STATUS (EMPLOYER ONLY) ====================
    
    @PostMapping("/close/{id}")
    public String closeJob(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            // Verify ownership
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            User employer = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            Job job = jobService.getJobById(id);
            if (!job.getEmployer().getId().equals(employer.getId())) {
                throw new RuntimeException("You don't have permission to close this job");
            }
            
            jobService.closeJob(id);
            redirectAttributes.addFlashAttribute("success", "Job closed successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to close job: " + e.getMessage());
        }
        return "redirect:/employer/jobs";
    }

    @PostMapping("/reopen/{id}")
    public String reopenJob(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            // Verify ownership
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            User employer = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            Job job = jobService.getJobById(id);
            if (!job.getEmployer().getId().equals(employer.getId())) {
                throw new RuntimeException("You don't have permission to reopen this job");
            }
            
            jobService.reopenJob(id);
            redirectAttributes.addFlashAttribute("success", "Job reopened successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to reopen job: " + e.getMessage());
        }
        return "redirect:/employer/jobs";
    }

    @PostMapping("/mark-filled/{id}")
    public String markJobAsFilled(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            // Verify ownership
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            User employer = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            Job job = jobService.getJobById(id);
            if (!job.getEmployer().getId().equals(employer.getId())) {
                throw new RuntimeException("You don't have permission to mark this job as filled");
            }
            
            jobService.markAsFilled(id);
            redirectAttributes.addFlashAttribute("success", "Job marked as filled successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to mark job as filled: " + e.getMessage());
        }
        return "redirect:/employer/jobs";
    }

    @PostMapping("/delete/{id}")
    public String deleteJob(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            // Verify ownership
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            User employer = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            Job job = jobService.getJobById(id);
            if (!job.getEmployer().getId().equals(employer.getId())) {
                throw new RuntimeException("You don't have permission to delete this job");
            }
            
            jobService.deleteJob(id);
            redirectAttributes.addFlashAttribute("success", "Job deleted successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to delete job: " + e.getMessage());
        }
        return "redirect:/employer/jobs";
    }

    // ==================== EMPLOYER JOB LISTINGS ====================
    
    @GetMapping("/my-jobs")
    public String myJobs(Model model) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User employer = userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        List<Job> jobs = jobService.getJobsByEmployer(employer.getId());
        model.addAttribute("jobs", jobs);
        return "employer/jobs";  // Redirect to employer jobs page
    }

    private void populateSavedJobIds(Model model) {
        if (isCurrentUserJobSeeker()) {
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            userRepository.findByEmail(email).ifPresent(user -> {
                java.util.List<com.revhire.model.SavedJob> savedJobs = savedJobRepository.findByUserId(user.getId());
                java.util.Set<Long> savedJobIds = savedJobs.stream()
                    .map(sj -> sj.getJob().getId())
                    .collect(java.util.stream.Collectors.toSet());
                model.addAttribute("savedJobIds", savedJobIds);
            });
        }
    }
    
    private void populateApplicationStatuses(Model model) {
        if (isCurrentUserJobSeeker()) {
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            userRepository.findByEmail(email).ifPresent(user -> {
                java.util.List<com.revhire.model.Application> applications = applicationRepository.findByUserId(user.getId());
                java.util.Map<Long, String> applicationStatuses = applications.stream()
                    .collect(java.util.stream.Collectors.toMap(
                        app -> app.getJob().getId(),
                        app -> app.getStatus()
                    ));
                model.addAttribute("applicationStatuses", applicationStatuses);
            });
        }
    }

    private boolean isCurrentUserEmployer() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof UserDetails) {
            return auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_EMPLOYER".equals(a.getAuthority()));
        }
        return false;
    }

    private boolean isCurrentUserJobSeeker() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof UserDetails) {
            return auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_JOBSEEKER".equals(a.getAuthority()));
        }
        return false;
    }
}
