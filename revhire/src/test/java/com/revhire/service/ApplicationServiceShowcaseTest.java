package com.revhire.service;

import com.revhire.model.Application;
import com.revhire.model.Job;
import com.revhire.model.User;
import com.revhire.model.Resume;
import com.revhire.repository.ApplicationRepository;
import com.revhire.repository.JobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ApplicationServiceShowcaseTest {

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private JobRepository jobRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private ApplicationService applicationService;

    private User seeker;
    private User employer;
    private Job job;
    private Resume resume;

    @BeforeEach
    public void setUp() {
        seeker = new User();
        seeker.setId(1L);
        seeker.setFirstName("John");
        seeker.setLastName("Seeker");

        employer = new User();
        employer.setId(2L);
        employer.setFirstName("Jane");
        employer.setLastName("Employer");

        job = new Job();
        job.setId(100L);
        job.setTitle("Java Developer");
        job.setEmployer(employer);
        job.setApplicationCount(0L);

        resume = new Resume();
        resume.setId(50L);
    }

    @Test
    public void showcase_ApplyForJob_Success() {
        // Arrange
        when(applicationRepository.existsByUserIdAndJobId(Long.valueOf(1L), Long.valueOf(100L))).thenReturn(false);
        when(applicationRepository.save(any(Application.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Application result = applicationService.applyForJob(seeker, job, resume, "I am a great dev!");

        // Assert
        assertNotNull(result);
        assertEquals(Long.valueOf(1L), job.getApplicationCount());
        
        // Verify notifications were triggered
        verify(notificationService, times(2)).sendNotification(any(), anyString(), anyString());
        System.out.println("✅ SUCCESS: Application logic verified. Application Count incremented and notifications sent.");
    }

    @Test
    public void showcase_ApplyForJob_AlreadyApplied() {
        // Arrange
        when(applicationRepository.existsByUserIdAndJobId(Long.valueOf(1L), Long.valueOf(100L))).thenReturn(true);

        // Act & Assert
        System.out.println("ℹ️ Testing duplicate application prevention...");
        assertThrows(RuntimeException.class, () -> {
            applicationService.applyForJob(seeker, job, resume, "Please hire me again!");
        });
    }
}
