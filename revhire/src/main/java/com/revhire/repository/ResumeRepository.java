package com.revhire.repository;

import com.revhire.model.Resume;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ResumeRepository extends JpaRepository<Resume, Long> {
    List<Resume> findByUserId(Long userId);
    Optional<Resume> findByUserIdAndIsPrimaryTrue(Long userId);
    void deleteByUserId(Long userId);
}