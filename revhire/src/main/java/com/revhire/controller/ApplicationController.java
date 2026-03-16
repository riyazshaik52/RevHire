package com.revhire.controller;

import com.revhire.model.Application;
import com.revhire.model.Job;
import com.revhire.model.Resume;
import com.revhire.model.User;
import com.revhire.repository.ApplicationRepository;
import com.revhire.repository.JobRepository;
import com.revhire.repository.ResumeRepository;
import com.revhire.repository.UserRepository;
import com.revhire.service.ApplicationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/applications")
public class ApplicationController {

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ResumeRepository resumeRepository;

    @GetMapping
    public String listApplications(@RequestParam(required = false) String status, Model model) {
        User user = getCurrentUser();
        if (user == null) {
            return "redirect:/auth/login";
        }

        List<Application> applications = applicationService.getUserApplications(user.getId());
        if (status != null && !status.trim().isEmpty()) {
            applications = applications.stream()
                .filter(app -> status.equalsIgnoreCase(app.getStatus()))
                .collect(Collectors.toList());
        }

        model.addAttribute("applications", applications);
        return "applications/list";
    }

    @GetMapping("/apply/{jobId}")
    public String showApplyForm(@PathVariable Long jobId, Model model, RedirectAttributes redirectAttributes) {
        User user = getCurrentUser();
        if (user == null) {
            return "redirect:/auth/login";
        }

        Job job = jobRepository.findById(jobId).orElse(null);
        if (job == null || !"ACTIVE".equals(job.getStatus())) {
            redirectAttributes.addFlashAttribute("error", "Job is not available.");
            return "redirect:/jobs";
        }

        if (applicationService.hasApplied(user.getId(), jobId)) {
            redirectAttributes.addFlashAttribute("error", "You already applied for this job.");
            return "redirect:/applications";
        }

        List<Resume> resumes = resumeRepository.findByUserId(user.getId());
        model.addAttribute("job", job);
        model.addAttribute("resumes", resumes);
        return "applications/apply";
    }

    @PostMapping("/apply/{jobId}")
    public String apply(@PathVariable Long jobId,
                        @RequestParam Long resumeId,
                        @RequestParam(required = false) String coverLetter,
                        RedirectAttributes redirectAttributes) {
        User user = getCurrentUser();
        if (user == null) {
            return "redirect:/auth/login";
        }

        Job job = jobRepository.findById(jobId).orElse(null);
        Resume resume = resumeRepository.findById(resumeId).orElse(null);

        if (job == null || !"ACTIVE".equals(job.getStatus())) {
            redirectAttributes.addFlashAttribute("error", "Job is not available.");
            return "redirect:/jobs";
        }

        if (resume == null || resume.getUser() == null || !resume.getUser().getId().equals(user.getId())) {
            redirectAttributes.addFlashAttribute("error", "Please select a valid resume.");
            return "redirect:/applications/apply/" + jobId;
        }

        try {
            applicationService.applyForJob(user, job, resume, coverLetter);
            redirectAttributes.addFlashAttribute("success", "Application submitted successfully.");
            return "redirect:/applications";
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/jobs/" + jobId;
        }
    }

    @PostMapping("/withdraw/{id}")
    public String withdraw(@PathVariable Long id,
                           @RequestParam(required = false) String reason,
                           RedirectAttributes redirectAttributes) {
        User user = getCurrentUser();
        if (user == null) {
            return "redirect:/auth/login";
        }

        Application application = applicationRepository.findById(id).orElse(null);
        if (application == null || application.getUser() == null || !application.getUser().getId().equals(user.getId())) {
            redirectAttributes.addFlashAttribute("error", "Application not found.");
            return "redirect:/applications";
        }

        try {
            applicationService.withdrawApplication(id, reason);
            redirectAttributes.addFlashAttribute("success", "Application withdrawn.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/applications";
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email).orElse(null);
    }
}
