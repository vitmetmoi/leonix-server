package com.example.myapp.features.generator;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobReferenceMediaRepository extends JpaRepository<JobReferenceMedia, Long> {
    List<JobReferenceMedia> findByJobId(Long jobId);
}
