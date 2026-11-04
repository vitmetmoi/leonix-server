package com.example.myapp.features.gateway;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AiModelRepository extends JpaRepository<AiModel, String> {
    List<AiModel> findByIsActiveTrueOrderByDisplayOrderAsc();
    List<AiModel> findByServiceTypeAndIsActiveTrueOrderByDisplayOrderAsc(String serviceType);
}
