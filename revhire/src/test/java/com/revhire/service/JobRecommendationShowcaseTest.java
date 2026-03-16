package com.revhire.service;

import com.revhire.model.Job;
import com.revhire.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class JobRecommendationShowcaseTest {

    @InjectMocks
    private JobService jobService;

    private User seeker;

    @BeforeEach
    public void setUp() {
        seeker = new User();
        seeker.setSkills("Java, Spring Boot, SQL");
        seeker.setPreferredRoles("Backend Developer, Software Engineer");
    }

    @Test
    public void showcase_RecommendationBySkill() {
        // Arrange
        Job job = new Job();
        job.setTitle("Frontend Wizard");
        job.setRequiredSkills("React, Java"); // Matches 'Java' in seeker skills

        // Act
        boolean isRecommended = jobService.isJobRecommendedForUser(job, seeker);

        // Assert
        assertTrue(isRecommended, "Job should be recommended based on skill match (Java)");
        System.out.println("✅ SUCCESS: Job recommendation via Skills logic verified.");
    }

    @Test
    public void showcase_RecommendationByRole() {
        // Arrange
        Job job = new Job();
        job.setTitle("Senior Backend Developer"); // Matches 'Backend Developer' in seeker roles
        job.setRequiredSkills("Go, AWS"); // No skill match

        // Act
        boolean isRecommended = jobService.isJobRecommendedForUser(job, seeker);

        // Assert
        assertTrue(isRecommended, "Job should be recommended based on role match");
        System.out.println("✅ SUCCESS: Job recommendation via Role match logic verified.");
    }

    @Test
    public void showcase_NoRecommendation() {
        // Arrange
        Job job = new Job();
        job.setTitle("UX Designer");
        job.setRequiredSkills("Figma, Adobe XD");

        // Act
        boolean isRecommended = jobService.isJobRecommendedForUser(job, seeker);

        // Assert
        assertFalse(isRecommended, "Job should NOT be recommended for non-matching profiles");
        System.out.println("✅ SUCCESS: System correctly filtered out non-matching job.");
    }
}
