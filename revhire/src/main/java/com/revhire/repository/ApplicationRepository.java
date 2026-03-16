package com.revhire.repository;

import com.revhire.model.Application;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {
    List<Application> findByUserId(Long userId);
    List<Application> findByJobId(Long jobId);
    List<Application> findByJobIdAndStatus(Long jobId, String status);
    List<Application> findByResumeId(Long resumeId);
    boolean existsByUserIdAndJobId(Long userId, Long jobId);
    long countByUserId(Long userId);
}
