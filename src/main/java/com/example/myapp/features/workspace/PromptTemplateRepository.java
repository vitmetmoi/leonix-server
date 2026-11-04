package com.example.myapp.features.workspace;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PromptTemplateRepository extends JpaRepository<PromptTemplate, Long> {
    List<PromptTemplate> findByIsSystemTrueOrderByCreatedAtDesc();

    @Query("SELECT p FROM PromptTemplate p WHERE " +
           "(:category IS NULL OR LOWER(p.category) = LOWER(:category)) AND " +
           "(:serviceType IS NULL OR LOWER(p.serviceType) = LOWER(:serviceType)) AND " +
           "(:query IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(p.promptVi) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "ORDER BY p.likesCount DESC, p.createdAt DESC")
    List<PromptTemplate> searchPrompts(
            @Param("category") String category,
            @Param("serviceType") String serviceType,
            @Param("query") String query
    );
}
