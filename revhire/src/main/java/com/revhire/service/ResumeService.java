package com.revhire.service;

import com.revhire.model.Resume;
import com.revhire.model.User;
import com.revhire.repository.ResumeRepository;
import com.revhire.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.annotation.Transactional;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import com.revhire.model.Application;
import com.revhire.repository.ApplicationRepository;

@Service
public class ResumeService {

    @Autowired
    private ResumeRepository resumeRepository;
    
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Value("${file.upload-dir}")
    private String uploadDir;

    public Resume uploadResume(User user, MultipartFile file, String originalFileName) throws IOException {
        System.out.println("========== RESUME SERVICE: UPLOAD STARTED ==========");
        System.out.println("User ID: " + (user != null ? user.getId() : "NULL"));
        System.out.println("User Email: " + (user != null ? user.getEmail() : "NULL"));
        System.out.println("Original File Name: " + originalFileName);
        System.out.println("File Size: " + file.getSize());
        System.out.println("Content Type: " + file.getContentType());
        
        // Validate user exists
        if (user == null) {
            System.err.println("ERROR: User is null");
            throw new IllegalArgumentException("User cannot be null");
        }
        
        if (user.getId() == null) {
            System.err.println("ERROR: User ID is null");
            throw new IllegalArgumentException("User ID cannot be null");
        }

        // Verify user exists in database
        boolean userExists = userRepository.existsById(user.getId());
        System.out.println("User exists in database: " + userExists);
        if (!userExists) {
            throw new RuntimeException("User with ID " + user.getId() + " does not exist in database");
        }

        // Validate file type
        String contentType = file.getContentType();
        if (!isValidFileType(contentType)) {
            System.err.println("ERROR: Invalid file type: " + contentType);
            throw new IllegalArgumentException("Only PDF and DOCX files are allowed");
        }

        // Validate file size (2MB = 2,097,152 bytes)
        if (file.getSize() > 2 * 1024 * 1024) {
            System.err.println("ERROR: File size exceeds limit: " + file.getSize());
            throw new IllegalArgumentException("File size exceeds 2MB limit");
        }

        // Create upload directory if it doesn't exist
        Path uploadPath = Paths.get(uploadDir);
        System.out.println("Upload path: " + uploadPath.toAbsolutePath());
        
        if (!Files.exists(uploadPath)) {
            System.out.println("Creating upload directory...");
            Files.createDirectories(uploadPath);
        }

        // Generate unique filename
        String fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
        String fileName = UUID.randomUUID().toString() + fileExtension;
        System.out.println("Generated filename: " + fileName);

        // Save file to disk
        Path filePath = uploadPath.resolve(fileName);
        System.out.println("Saving file to: " + filePath);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        System.out.println("File saved successfully");

        // Create resume record
        Resume resume = new Resume();
        resume.setUser(user);
        resume.setFileName(fileName);
        resume.setOriginalFileName(originalFileName);
        resume.setFileType(contentType);
        resume.setFileSize(file.getSize());
        resume.setFilePath(uploadDir + "/" + fileName);
        resume.setIsPrimary(false);
        resume.setUploadedAt(LocalDateTime.now());

        // If this is the first resume, set as primary
        List<Resume> existingResumes = resumeRepository.findByUserId(user.getId());
        System.out.println("Existing resumes count: " + existingResumes.size());
        
        if (existingResumes.isEmpty()) {
            resume.setIsPrimary(true);
            System.out.println("Setting as primary resume (first upload)");
        }

        System.out.println("===== INSERTING RESUME =====");
        System.out.println("ID: " + resume.getId());
        System.out.println("User ID: " + (resume.getUser() != null ? resume.getUser().getId() : "NULL"));
        System.out.println("File Name: " + resume.getFileName());
        System.out.println("File Path: " + resume.getFilePath());
        System.out.println("File Size: " + resume.getFileSize());
        System.out.println("File Type: " + resume.getFileType());
        System.out.println("Is Primary: " + resume.getIsPrimary());
        System.out.println("Original File Name: " + resume.getOriginalFileName());
        System.out.println("Uploaded At: " + resume.getUploadedAt());

        System.out.println("Saving resume to database...");
        Resume savedResume = resumeRepository.save(resume);
        System.out.println("Resume saved with ID: " + savedResume.getId());
        
        return savedResume;
    }

    public List<Resume> getUserResumes(Long userId) {
        System.out.println("Fetching resumes for user ID: " + userId);
        List<Resume> resumes = resumeRepository.findByUserId(userId);
        System.out.println("Found " + resumes.size() + " resumes");
        return resumes;
    }

    @Transactional
    public void deleteResume(Long resumeId) throws IOException {
        System.out.println("Deleting resume with ID: " + resumeId);
        
        Resume resume = resumeRepository.findById(resumeId)
            .orElseThrow(() -> new RuntimeException("Resume not found with ID: " + resumeId));
        
        System.out.println("Found resume: " + resume.getFileName());
        Long userId = resume.getUser().getId();
        boolean wasPrimary = resume.getIsPrimary();

        // 1. Handle Applications linked to this resume
        List<Application> linkedApplications = applicationRepository.findByResumeId(resumeId);
        System.out.println("Found " + linkedApplications.size() + " linked applications. Setting resume to null.");
        for (Application app : linkedApplications) {
            app.setResume(null);
            applicationRepository.save(app);
        }

        // 2. Delete database record
        resumeRepository.delete(resume);
        System.out.println("Database record deleted.");

        // 3. If was primary, set another resume as primary if available
        if (wasPrimary) {
            List<Resume> remainingResumes = resumeRepository.findByUserId(userId);
            if (!remainingResumes.isEmpty()) {
                Resume nextPrimary = remainingResumes.get(0);
                nextPrimary.setIsPrimary(true);
                resumeRepository.save(nextPrimary);
                System.out.println("New primary resume set: " + nextPrimary.getId());
            }
        }

        // 4. Delete file from disk
        Path filePath = Paths.get(resume.getFilePath());
        System.out.println("Deleting file from disk: " + filePath);
        try {
            Files.deleteIfExists(filePath);
            System.out.println("File deleted successfully from disk.");
        } catch (IOException e) {
            System.err.println("Warning: Could not delete file from disk: " + e.getMessage());
            // We don't throw here to avoid rolling back the DB deletion if the file is already gone 
            // or there's a minor disk issue, but we logged it.
        }
    }

    public void setPrimaryResume(Long userId, Long resumeId) {
        System.out.println("Setting resume ID " + resumeId + " as primary for user ID " + userId);
        
        // Reset all resumes to non-primary
        List<Resume> userResumes = resumeRepository.findByUserId(userId);
        System.out.println("Found " + userResumes.size() + " resumes to update");
        
        userResumes.forEach(r -> r.setIsPrimary(false));
        resumeRepository.saveAll(userResumes);
        
        // Set selected resume as primary
        Resume primaryResume = resumeRepository.findById(resumeId)
            .orElseThrow(() -> new RuntimeException("Resume not found with ID: " + resumeId));
        primaryResume.setIsPrimary(true);
        resumeRepository.save(primaryResume);
        
        System.out.println("Primary resume updated successfully");
    }

    private boolean isValidFileType(String contentType) {
        return contentType.equals("application/pdf") ||
               contentType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
    }
}