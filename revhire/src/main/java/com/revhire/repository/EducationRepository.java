package com.revhire.repository;

import com.revhire.model.Education;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface EducationRepository extends JpaRepository<Education, Long> {
    List<Education> findByUserId(Long userId);
    List<Education> findByUserIdOrderByEndYearDesc(Long userId);
}