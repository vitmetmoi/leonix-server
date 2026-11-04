package com.example.myapp.features.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserLlmConfigRepository extends JpaRepository<UserLlmConfig, Long> {
    Optional<UserLlmConfig> findByUserId(Long userId);
}
