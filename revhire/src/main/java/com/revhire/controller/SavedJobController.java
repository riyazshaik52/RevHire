package com.revhire.controller;

import com.revhire.model.SavedJob;
import com.revhire.model.User;
import com.revhire.model.Job;
import com.revhire.repository.SavedJobRepository;
import com.revhire.repository.UserRepository;
import com.revhire.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/saved-jobs")
public class SavedJobController {

    @Autowired
    private SavedJobRepository savedJobRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JobRepository jobRepository;

    @GetMapping
    public String viewSavedJobs(Model model) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email).orElse(null);
        
        if (user != null) {
            List<SavedJob> savedJobs = savedJobRepository.findByUserId(user.getId());
            model.addAttribute("savedJobs", savedJobs);
        }
        
        return "saved-jobs/list";
    }

    @PostMapping("/toggle/{jobId}")
    @ResponseBody
    public java.util.Map<String, Object> toggleSavedJob(@PathVariable Long jobId) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email).orElse(null);
        Job job = jobRepository.findById(jobId).orElse(null);
        
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        
        if (user != null && job != null) {
            java.util.Optional<SavedJob> existing = savedJobRepository.findByUserIdAndJobId(user.getId(), jobId);
            if (existing.isPresent()) {
                savedJobRepository.delete(existing.get());
                response.put("saved", false);
                response.put("message", "Job removed from favorites");
            } else {
                SavedJob savedJob = new SavedJob();
                savedJob.setUser(user);
                savedJob.setJob(job);
                savedJobRepository.save(savedJob);
                response.put("saved", true);
                response.put("message", "Job added to favorites");
            }
            response.put("success", true);
        } else {
            response.put("success", false);
            response.put("message", "Error toggling job status");
        }
        return response;
    }

    @PostMapping("/remove/{jobId}")
    public String removeSavedJob(@PathVariable Long jobId, RedirectAttributes redirectAttributes) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email).orElse(null);
        
        if (user != null) {
            savedJobRepository.deleteByUserIdAndJobId(user.getId(), jobId);
            redirectAttributes.addFlashAttribute("success", "Job removed from saved list!");
        }
        
        return "redirect:/saved-jobs";
    }
}