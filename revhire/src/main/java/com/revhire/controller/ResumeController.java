package com.revhire.controller;

import com.revhire.model.Resume;
import com.revhire.model.User;
import com.revhire.repository.ApplicationRepository;
import com.revhire.repository.ResumeRepository;
import com.revhire.repository.UserRepository;
import com.revhire.service.ResumeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Controller
@RequestMapping("/resume")
public class ResumeController {

    @Autowired
    private ResumeService resumeService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ResumeRepository resumeRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @GetMapping
    public String viewResumes(Model model) {
        System.out.println("========== VIEW RESUMES ==========");
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        System.out.println("User email from security: " + email);
        
        User user = userRepository.findByEmail(email).orElse(null);
        
        if (user == null) {
            System.out.println("User not found with email: " + email);
            return "redirect:/auth/login";
        }
        
        System.out.println("User found with ID: " + user.getId());
        List<Resume> resumes = resumeService.getUserResumes(user.getId());
        System.out.println("Found " + resumes.size() + " resumes");
        
        model.addAttribute("resumes", resumes);
        return "resume/list";
    }

    @GetMapping("/upload")
    public String showUploadForm() {
        System.out.println("========== SHOW UPLOAD FORM ==========");
        return "resume/upload";
    }

    @PostMapping("/upload")
    public String uploadResume(@RequestParam("file") MultipartFile file,
                               RedirectAttributes redirectAttributes) {
        System.out.println("========== UPLOAD RESUME ==========");
        
        try {
            // Get current user email
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            System.out.println("User email from security: " + email);
            
            // Find user in database
            User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
            
            System.out.println("User found with ID: " + user.getId());
            System.out.println("User name: " + user.getFirstName() + " " + user.getLastName());
            
            // Check if file is empty
            if (file.isEmpty()) {
                System.out.println("File is empty");
                redirectAttributes.addFlashAttribute("error", "Please select a file to upload");
                return "redirect:/resume/upload";
            }
            
            // Log file details
            System.out.println("File name: " + file.getOriginalFilename());
            System.out.println("File size: " + file.getSize() + " bytes");
            System.out.println("File type: " + file.getContentType());
            
            // Upload resume
            resumeService.uploadResume(user, file, file.getOriginalFilename());
            System.out.println("Resume uploaded successfully!");
            
            redirectAttributes.addFlashAttribute("success", "Resume uploaded successfully!");
            
        } catch (IllegalArgumentException e) {
            System.out.println("Validation error: " + e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/resume/upload";
        } catch (Exception e) {
            System.out.println("Error uploading resume: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Upload failed: " + e.getMessage());
        }
        
        return "redirect:/resume";
    }

    @PostMapping("/delete/{id}")
    public String deleteResume(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        System.out.println("========== DELETE RESUME ==========");
        System.out.println("Resume ID to delete: " + id);
        
        try {
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Logged in user not found"));

            Resume resume = resumeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Resume not found"));

            // Ownership check
            if (resume.getUser() == null || !resume.getUser().getId().equals(currentUser.getId())) {
                System.out.println("Unauthorized deletion attempt. User ID: " + currentUser.getId() + ", Resume Owner ID: " + (resume.getUser() != null ? resume.getUser().getId() : "null"));
                redirectAttributes.addFlashAttribute("error", "You are not authorized to delete this resume.");
                return "redirect:/resume";
            }

            resumeService.deleteResume(id);
            System.out.println("Resume deleted successfully");
            redirectAttributes.addFlashAttribute("success", "Resume deleted successfully!");
        } catch (Exception e) {
            System.out.println("Error deleting resume: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Delete failed: " + e.getMessage());
        }
        
        return "redirect:/resume";
    }

    @PostMapping("/set-primary/{id}")
    public String setPrimaryResume(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        System.out.println("========== SET PRIMARY RESUME ==========");
        System.out.println("Resume ID to set as primary: " + id);
        
        try {
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            System.out.println("User email from security: " + email);
            
            User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
            
            System.out.println("User found with ID: " + user.getId());
            
            resumeService.setPrimaryResume(user.getId(), id);
            System.out.println("Primary resume updated successfully");
            
            redirectAttributes.addFlashAttribute("success", "Primary resume updated!");
        } catch (Exception e) {
            System.out.println("Error setting primary resume: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Operation failed: " + e.getMessage());
        }
        
        return "redirect:/resume";
    }

    @GetMapping("/download/{id}")
    @ResponseBody
    public ResponseEntity<Resource> downloadResume(@PathVariable Long id) throws MalformedURLException {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("User not found"));

        Resume resume = resumeRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Resume not found"));

        boolean isOwner = resume.getUser() != null && resume.getUser().getId().equals(currentUser.getId());
        boolean isEmployerLinked = "EMPLOYER".equals(currentUser.getUserType())
            && applicationRepository.findByResumeId(id).stream().anyMatch(app ->
                app.getJob() != null
                    && app.getJob().getEmployer() != null
                    && app.getJob().getEmployer().getId().equals(currentUser.getId()));

        if (!isOwner && !isEmployerLinked) {
            return ResponseEntity.status(403).build();
        }

        Path filePath = Paths.get(resume.getFilePath()).toAbsolutePath().normalize();
        Resource resource = new UrlResource(filePath.toUri());
        if (!resource.exists() || !resource.isReadable()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resume.getOriginalFileName() + "\"")
            .body(resource);
    }
}
