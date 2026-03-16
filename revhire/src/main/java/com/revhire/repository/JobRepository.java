package com.revhire.repository;

import com.revhire.model.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface JobRepository extends JpaRepository<Job, Long> {
    List<Job> findByEmployerId(Long employerId);
    List<Job> findByStatus(String status);
    
    @Query("SELECT j FROM Job j WHERE " +
           "(:title IS NULL OR LOWER(j.title) LIKE LOWER(CONCAT('%', :title, '%'))) AND " +
           "(:location IS NULL OR LOWER(j.location) LIKE LOWER(CONCAT('%', :location, '%'))) AND " +
           "(:employmentType IS NULL OR j.employmentType = :employmentType) AND " +
           "(:experienceLevel IS NULL OR j.experienceLevel = :experienceLevel) AND " +
           "(:minSalary IS NULL OR j.minSalary >= :minSalary) AND " +
           "(:maxSalary IS NULL OR j.maxSalary <= :maxSalary) AND " +
           "(:companyName IS NULL OR j.employer.id IN (SELECT c.user.id FROM Company c WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :companyName, '%')))) AND " +
           "(:datePosted IS NULL OR j.createdAt >= :datePosted) AND " +
           "j.status = 'ACTIVE'")
    List<Job> searchJobs(@Param("title") String title,
                        @Param("location") String location,
                        @Param("employmentType") String employmentType,
                        @Param("experienceLevel") String experienceLevel,
                        @Param("minSalary") Double minSalary,
                        @Param("maxSalary") Double maxSalary,
                        @Param("companyName") String companyName,
                        @Param("datePosted") java.time.LocalDateTime datePosted);
}