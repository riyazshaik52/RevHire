package com.revhire.service;

import com.revhire.model.Application;
import com.revhire.model.Job;
import com.revhire.model.User;
import com.revhire.model.Resume;
import com.revhire.repository.ApplicationRepository;
import com.revhire.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ApplicationService {

    @Autowired
    private ApplicationRepository applicationRepository;
    
    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private NotificationService notificationService;

    public Application applyForJob(User user, Job job, Resume resume, String coverLetter) {
        // Check if already applied
        if (applicationRepository.existsByUserIdAndJobId(user.getId(), job.getId())) {
            throw new RuntimeException("You have already applied for this job");
        }
        
        Application application = new Application();
        application.setUser(user);
        application.setJob(job);
        application.setResume(resume);
        application.setCoverLetter(coverLetter);
        application.setStatus("APPLIED");
        
        // Increment application count on job
        job.setApplicationCount(job.getApplicationCount() + 1);
        jobRepository.save(job);
        Application savedApplication = applicationRepository.save(application);

        // SEND NOTIFICATION TO JOB SEEKER
        notificationService.sendNotification(
                user,
                "You applied for job: " + job.getTitle(),
                "APPLIED"
        );

        // SEND NOTIFICATION TO EMPLOYER
        if (job.getEmployer() != null) {
            notificationService.sendNotification(
                    job.getEmployer(),
                    user.getFirstName() + " " + user.getLastName() + " applied for your job: " + job.getTitle(),
                    "NEW_APPLICATION"
            );
        }

        return savedApplication;
    }

    public List<Application> getUserApplications(Long userId) {
        return applicationRepository.findByUserId(userId);
    }

    public List<Application> getJobApplications(Long jobId) {
        return applicationRepository.findByJobId(jobId);
    }

    @Transactional
    public Application updateApplicationStatus(Long applicationId, String status, String notes) {

        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found"));

        application.setStatus(status);

        if (notes != null) {
            application.setEmployerNotes(notes);
        }

        application.setUpdatedAt(LocalDateTime.now());

        Application updatedApplication = applicationRepository.save(application);

        User applicant = application.getUser();

        if (status.equalsIgnoreCase("SHORTLISTED")) {
            notificationService.sendNotification(
                    applicant,
                    "Congratulations! You have been shortlisted for: " + application.getJob().getTitle(),
                    "SHORTLISTED"
            );
        } else if (status.equalsIgnoreCase("REJECTED")) {
            notificationService.sendNotification(
                    applicant,
                    "Update on your application: We have decided not to move forward with your application for " + application.getJob().getTitle(),
                    "REJECTED"
            );
        } else {
            notificationService.sendNotification(
                    applicant,
                    "Your application status for '" + application.getJob().getTitle() + "' has been updated to: " + status,
                    "STATUS_UPDATE"
            );
        }

        return updatedApplication;
    }

    @Transactional
    public Application withdrawApplication(Long applicationId, String reason) {
        Application application = applicationRepository.findById(applicationId)
            .orElseThrow(() -> new RuntimeException("Application not found"));
            
        // Security check: Only allow withdrawal if not yet completely processed
        String currentStatus = application.getStatus();
        if (!"APPLIED".equals(currentStatus) && !"UNDER_REVIEW".equals(currentStatus)) {
            throw new RuntimeException("Cannot withdraw application that is " + currentStatus);
        }
        
        application.setStatus("WITHDRAWN");
        application.setWithdrawalReason(reason);
        application.setUpdatedAt(LocalDateTime.now());
        
        return applicationRepository.save(application);
    }

    public List<Application> getApplicationsByStatus(Long jobId, String status) {
        return applicationRepository.findByJobIdAndStatus(jobId, status);
    }

    public boolean hasApplied(Long userId, Long jobId) {
        return applicationRepository.existsByUserIdAndJobId(userId, jobId);
    }
}