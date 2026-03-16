package com.revhire.service;

import com.revhire.model.Job;
import com.revhire.model.User;
import com.revhire.model.Company;
import com.revhire.repository.JobRepository;
import com.revhire.repository.UserRepository;
import com.revhire.repository.CompanyRepository;
import com.revhire.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class JobService {

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private CompanyRepository companyRepository;

    public Job createJob(Job job, User employer) {
        job.setEmployer(employer);
        job.setStatus("ACTIVE");
        Job savedJob = jobRepository.save(job);
        
        // Proactively notify matching job seekers
        try {
            List<User> jobSeekers = userRepository.findByUserType("JOBSEEKER");
            Company employerCompany = companyRepository.findByUserId(employer.getId()).orElse(null);
            String companyName = (employerCompany != null ? employerCompany.getName() : (employer.getFirstName() + " " + employer.getLastName()));

            for (User seeker : jobSeekers) {
                if (isJobRecommendedForUser(savedJob, seeker)) {
                    String message = String.format("Recommended: %s at %s matches your profile preferences!", 
                        savedJob.getTitle(), 
                        companyName);
                    
                    notificationService.sendNotification(seeker, message, "JOB_RECOMMENDATION");
                }
            }
        } catch (Exception e) {
            // Log error but don't fail job creation if notification fails
            System.err.println("Error sending proactive job notifications: " + e.getMessage());
        }
        
        return savedJob;
    }

    public Job updateJob(Job job) {
        return jobRepository.save(job);
    }

    public void deleteJob(Long jobId) {
        jobRepository.deleteById(jobId);
    }

    public Job getJobById(Long jobId) {
        return jobRepository.findById(jobId)
            .orElseThrow(() -> new RuntimeException("Job not found"));
    }

    public List<Job> getJobsByEmployer(Long employerId) {
        return jobRepository.findByEmployerId(employerId);
    }

    public List<Job> getAllActiveJobs() {
        return jobRepository.findByStatus("ACTIVE");
    }

    public List<Job> searchJobs(String title, String location, String employmentType, 
                               String experienceLevel, Double minSalary, Double maxSalary,
                               String companyName, java.time.LocalDateTime datePosted) {
        return jobRepository.searchJobs(title, location, employmentType, 
                                       experienceLevel, minSalary, maxSalary,
                                       companyName, datePosted);
    }

    public Job closeJob(Long jobId) {
        Job job = getJobById(jobId);
        job.setStatus("CLOSED");
        return jobRepository.save(job);
    }

    public Job reopenJob(Long jobId) {
        Job job = getJobById(jobId);
        job.setStatus("ACTIVE");
        return jobRepository.save(job);
    }
    
    public List<Job> getRecommendedJobs(User user) {
        // Get all active jobs to filter
        List<Job> activeJobs = getAllActiveJobs();
        
        return activeJobs.stream()
                .filter(job -> isJobRecommendedForUser(job, user))
                // Sort by newest first to keep recommendations relevant
                .sorted((j1, j2) -> j2.getCreatedAt().compareTo(j1.getCreatedAt()))
                .limit(5) // Max 5 recommendations
                .collect(java.util.stream.Collectors.toList());
    }

    public boolean isJobRecommendedForUser(Job job, User user) {
        boolean hasSkills = user != null && user.getSkills() != null && !user.getSkills().trim().isEmpty();
        boolean hasRoles = user != null && user.getPreferredRoles() != null && !user.getPreferredRoles().trim().isEmpty();
        
        if (!hasSkills && !hasRoles) {
            return false;
        }

        // Parse user skills
        String[] userSkills = hasSkills ? user.getSkills().toLowerCase().split(",") : new String[0];
        for (int i = 0; i < userSkills.length; i++) {
            userSkills[i] = userSkills[i].trim();
        }

        // Parse user preferred roles
        String[] userRoles = hasRoles ? user.getPreferredRoles().toLowerCase().split(",") : new String[0];
        for (int i = 0; i < userRoles.length; i++) {
            userRoles[i] = userRoles[i].trim();
        }

        // 1) Match by Preferred Roles (Title, Industry, Employment Type)
        if (hasRoles) {
            String jobTitle = job.getTitle() != null ? job.getTitle().toLowerCase() : "";
            String jobIndustry = job.getIndustry() != null ? job.getIndustry().toLowerCase() : "";
            String jobType = job.getEmploymentType() != null ? job.getEmploymentType().toLowerCase() : "";
            
            for (String role : userRoles) {
                if (!role.isEmpty() && (jobTitle.contains(role) || jobIndustry.contains(role) || jobType.contains(role))) {
                    return true;
                }
            }
        }
        
        // 2) Match by Skills
        if (hasSkills && job.getRequiredSkills() != null && !job.getRequiredSkills().trim().isEmpty()) {
            String jobSkillsStr = job.getRequiredSkills().toLowerCase();
            for (String userSkill : userSkills) {
                if (!userSkill.isEmpty() && jobSkillsStr.contains(userSkill)) {
                    return true;
                }
            }
        }
        
        return false;
    }

    public Job markAsFilled(Long jobId) {
        Job job = getJobById(jobId);
        job.setStatus("FILLED");
        return jobRepository.save(job);
    }

    public void incrementViewCount(Long jobId) {
        Job job = getJobById(jobId);
        job.setViewCount(job.getViewCount() + 1);
        jobRepository.save(job);
    }

    public void incrementApplicationCount(Long jobId) {
        Job job = getJobById(jobId);
        job.setApplicationCount(job.getApplicationCount() + 1);
        jobRepository.save(job);
    }
}